package searchengine.controllers;
import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.Site;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.response.FalseResponse;
import searchengine.response.TrueResponse;
import searchengine.services.IndexingService;

import searchengine.services.LemmaCounter;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;


import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
//@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final SearchService searchService;
    private final IndexingService indexingService;
    private final SitesList sitesList;
    private final LemmaCounter lemmaCounter;
    private final AtomicBoolean indexingProcessing = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ApiController(StatisticsService statisticsService,SitesList sitesList, IndexingService indexingService, LemmaCounter lemmaCounter,SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.sitesList = sitesList;
        this.lemmaCounter = lemmaCounter;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Object> startIndexing() throws InterruptedException {
        if(indexingProcessing.get()){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new FalseResponse("Индексация уже запущена"));
        }else {
            executor.submit(() ->{
                indexingProcessing.set(true);
                try {
                    indexingService.startPageIndexing(indexingProcessing);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            });

            return ResponseEntity.status(HttpStatus.OK).body(new TrueResponse());
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Object> stopIndexing(){
        if (!indexingProcessing.get()) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(new FalseResponse("Индексация не запущена"));
        } else {
            indexingProcessing.set(false);

            return ResponseEntity.status(HttpStatus.OK).body(new TrueResponse());
        }
    }


    @PostMapping("/indexPage")
    public ResponseEntity<Object> indexPage(@RequestParam String url) throws IOException, InterruptedException {

        List<SiteEntity> siteEntityList = indexingService.getSiteRepository().findAll();
        for(SiteEntity siteEntity : siteEntityList){

            if(url.contains(siteEntity.getUrl())){
                Document doc = Jsoup.connect(url).timeout(100000).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").referrer("http://www.google.com").get();
                Elements elements = doc.select("a[href]");
                Connection.Response response = Jsoup.connect(url).execute();
                PageEntity pageEntity = new PageEntity();
                pageEntity.setSiteId(siteEntity);
                pageEntity.setPath(url);
                pageEntity.setCode(response.statusCode());
                pageEntity.setContent(doc.toString());
                try{
                    lemmaCounter.getPageRepository().save(pageEntity);
                    lemmaCounter.saveLemmaToRepository(pageEntity.getPath());
                }
                catch (Exception e){
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new FalseResponse("Кодировка сайта не подходит"));
                }

                return ResponseEntity.status(HttpStatus.OK).body( new TrueResponse());
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new FalseResponse("Данная страница находится за пределами сайтов указанных в конфигурационном файле"));


    }


    @GetMapping("/search")
    public ResponseEntity<Object> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String site,
            @RequestParam(required = false, defaultValue = "4") Integer offset,
            @RequestParam(required = false, defaultValue = "20") Integer limit
    ) throws IOException, InterruptedException {
        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().body(new FalseResponse("Задан пустой поисковой запрос"));
        }
        return searchService.search(query,site,offset,limit);
    }


}
