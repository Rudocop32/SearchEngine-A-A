package searchengine.services;


import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.response.FalseResponse;
import searchengine.response.TrueResponse;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class IndexingService {

    private final SiteRepository siteRepository;

    private final PageRepository pageRepository;


    private final SitesList sitesList;

    private final List<SiteEntity> siteEntityList;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private AtomicBoolean indexingProcessing;
    private final ForkJoinPool forkJoinPool;

    private final LemmaCounter lemmaCounter;
    public IndexingService(SiteRepository siteRepository,LemmaCounter lemmaCounter, PageRepository pageRepository, SitesList sitesList, List<SiteEntity> siteEntityList, LemmaRepository lemmaRepository, IndexRepository indexRepository) throws IOException {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.sitesList = sitesList;
        this.siteEntityList = siteEntityList;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.lemmaCounter = lemmaCounter;
        forkJoinPool = new ForkJoinPool();
    }


    @Async
    public void startPageIndexing(AtomicBoolean indexingProcessing) throws InterruptedException {
        this.indexingProcessing = indexingProcessing;
        try {
            deleteSiteAndPageIfExist();
            addSitesToRepository();
            indexAllPages();
        } catch (RuntimeException | InterruptedException ex) {
            indexingProcessing.set(false);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void deleteSiteAndPageIfExist() {
        List<SiteEntity> siteEntitiesFromRepository = siteRepository.findAll();
        for (SiteEntity siteEntity : siteEntitiesFromRepository) {
            for (Site site : sitesList.getSites()) {
                if (siteEntity.getUrl().equals(site.getUrl())) {
                    siteRepository.deleteById(siteEntity.getId());
                }
            }
        }
    }

    private void addSitesToRepository() {
        for (Site site : sitesList.getSites()) {
            SiteEntity siteEntity = new SiteEntity();
            siteEntity.setName(site.getName());
            siteEntity.setUrl(site.getUrl());
            siteEntity.setStatusType(Status.INDEXING);
            siteRepository.save(siteEntity);
        }
    }

    public ResponseEntity<Object> indexPage(String url) throws IOException {
        List<SiteEntity> siteEntityList = siteRepository.findAll();
        for(SiteEntity siteEntity : siteEntityList){
            if(url.contains(siteEntity.getUrl())){
                Document doc = Jsoup.connect(url).timeout(100000).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").referrer("http://www.google.com").get();
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

    public void refreshPage(PageEntity pageEntity, String url) {
        try {
            lemmaCounter.saveLemmaToRepository(url);
        } catch (IOException e) {
            e.fillInStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void indexAllPages() throws InterruptedException, IOException {
        siteEntityList.addAll(siteRepository.findAll());
        List<PageIndexing> pageIndexingList = new ArrayList<>();
        for (SiteEntity siteEntity : siteEntityList) {
                ConcurrentHashMap<String, Boolean> inputLinks = new ConcurrentHashMap<>();
                inputLinks.put(siteEntity.getUrl(), false);
                try {
                    PageIndexing pageIndexing = new PageIndexing(siteRepository, inputLinks, siteEntity.getUrl(), 0, siteEntity, indexingProcessing, lemmaCounter);
                    pageIndexingList.add(pageIndexing);
                    ArrayList<PageEntity> pages = forkJoinPool.invoke(pageIndexing);
                } catch (SecurityException ex) {
                    setSiteIfError(siteEntity);
                }
            if (!indexingProcessing.get()) {
                    setSiteIfStopped(siteEntity);
                }
        }
        for (PageIndexing pageIndexing : pageIndexingList) {
            if(!indexingProcessing.get()){
                break;
            }
            pageIndexing.join();
        }
        if (!indexingProcessing.get()) {
            setAllSiteAreIndexed();
        }
        indexingProcessing.set(false);
        System.out.println("INDEXING FINISHED!!!!!!!!!!!!");
    }

    private void setSiteIfError(SiteEntity siteEntity){
        SiteEntity sitePage = siteRepository.findById(siteEntity.getId()).get();
        sitePage.setStatusType(Status.FAILED);
        sitePage.setLastError("Indexing stopped by user");
        siteRepository.save(sitePage);
    }
    private void setSiteIfStopped(SiteEntity siteEntity){
        SiteEntity sitePage = siteRepository.findById(siteEntity.getId()).get();
        sitePage.setStatusType(Status.FAILED);
        sitePage.setLastError("Indexing stopped by user");
        siteRepository.save(sitePage);
    }
    private void setAllSiteAreIndexed(){
        for (SiteEntity indexedSite : siteRepository.findAll()) {
            if (indexedSite.getStatusType().equals(Status.INDEXED)) {
                continue;
            }
            indexedSite.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
            indexedSite.setStatusType(Status.FAILED);
            siteRepository.save(indexedSite);
        }
    }
}

