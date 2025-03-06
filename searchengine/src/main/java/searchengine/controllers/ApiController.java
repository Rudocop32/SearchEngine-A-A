package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.response.FalseResponse;
import searchengine.response.TrueResponse;
import searchengine.services.IndexingService;
import searchengine.services.LemmaCounter;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final SearchService searchService;
    private final IndexingService indexingService;
    private final AtomicBoolean indexingProcessing = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ApiController(StatisticsService statisticsService, IndexingService indexingService,SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new TrueResponse());
        }
    }


    @PostMapping("/indexPage")
    public ResponseEntity<Object> indexPage(@RequestParam String url) throws IOException, InterruptedException {
       return indexingService.indexPage(url);
    }


    @GetMapping("/search")
    public ResponseEntity<Object> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String site,
            @RequestParam(required = false, defaultValue = "0") Integer offset,
            @RequestParam(required = false, defaultValue = "10") Integer limit
    ) throws IOException, InterruptedException {
        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().body(new FalseResponse("Задан пустой поисковой запрос"));
        }
        return searchService.search(query,site,offset,limit);
    }
}
