package searchengine.services;


import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.data.domain.Page;
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



    public IndexingService(SiteRepository siteRepository, PageRepository pageRepository, SitesList sitesList, List<SiteEntity> siteEntityList, LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.sitesList = sitesList;
        this.siteEntityList = siteEntityList;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;

        forkJoinPool = new ForkJoinPool();

    }

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

    public void refreshPage(PageEntity pageEntity, String url) {
        try {
            LemmaCounter lemmaCounter = new LemmaCounter(pageRepository, lemmaRepository, indexRepository);
            lemmaCounter.saveLemmaToRepository(url);
        } catch (IOException e) {
            e.fillInStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void indexAllPages() throws InterruptedException, IOException {
        siteEntityList.addAll(siteRepository.findAll());

        List<Thread> indexingThreadList = new ArrayList<>();
        for (SiteEntity siteEntity : siteEntityList) {
            Runnable siteIndexing = () -> {
                ConcurrentHashMap<String, Boolean> inputLinks = new ConcurrentHashMap<>();

                PageEntity pageEntity = new PageEntity();

                inputLinks.put(siteEntity.getUrl(), false);
                try {
                    LemmaCounter lemmaCounter = new LemmaCounter(pageRepository,lemmaRepository,indexRepository);
                    PageIndexing pageIndexing = new PageIndexing(siteRepository, inputLinks, siteEntity.getUrl(), siteEntity.getUrl(), 0, siteEntity, /*pageEntity,*/indexingProcessing,lemmaCounter);

                    ArrayList<PageEntity> pages =  pageIndexing.compute();



                } catch (SecurityException ex) {
                    SiteEntity sitePage = siteRepository.findById(siteEntity.getId()).get();
                    sitePage.setStatusType(Status.FAILED);
                    sitePage.setLastError(ex.getMessage());
                    siteRepository.save(sitePage);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                if (!indexingProcessing.get()) {
                    SiteEntity sitePage = siteRepository.findById(siteEntity.getId()).get();
                    sitePage.setStatusType(Status.FAILED);
                    sitePage.setLastError("Indexing stopped by user");
                    siteRepository.save(sitePage);
                }


            };


            Thread thread = new Thread(siteIndexing);
            indexingThreadList.add(thread);
            thread.start();
        }
        for (Thread thread : indexingThreadList) {
            thread.join();
        }
        List<SiteEntity> indexedSiteList = new ArrayList<>();
        for (SiteEntity indexedSite : siteRepository.findAll()) {


            indexedSite.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
            indexedSite.setStatusType(Status.INDEXED);
            siteRepository.save(indexedSite);


        }
        indexingProcessing.set(false);
        System.out.println("INDEXING FINISHED!!!!!!!!!!!!");

    }

    public SiteRepository getSiteRepository() {
        return siteRepository;
    }

    public PageRepository getPageRepository() {
        return pageRepository;
    }
}

