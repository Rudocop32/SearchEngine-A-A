package searchengine.services;

import org.jsoup.Connection;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.Status;

import searchengine.repository.SiteRepository;
import java.io.IOException;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicBoolean;


public class PageIndexing extends RecursiveAction {
    private final SiteRepository siteRepository;
    private final ConcurrentHashMap<String, Boolean> lookedLinks;
    private final String url;
    private final int level;
    private final SiteEntity siteEntity;
    private final AtomicBoolean indexingProcessing;
    private final LemmaCounter lemmaCounter;

    public PageIndexing(SiteRepository siteRepository, ConcurrentHashMap<String, Boolean> lookedLinks, String url,  int level, SiteEntity siteEntity, AtomicBoolean indexingProcessing, LemmaCounter lemmaCounter) {
        this.siteRepository = siteRepository;
        this.lookedLinks = lookedLinks;
        this.url = url;
        this.level = level;
        this.siteEntity = siteEntity;
        this.lemmaCounter = lemmaCounter;
        this.indexingProcessing = indexingProcessing;
    }
    @Override
    protected void compute() {

        try {
            Document doc = Jsoup.connect(url).timeout(100000).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").referrer("http://www.google.com").get();
            Elements elements = doc.select("a[href]");
            PageEntity pageEntity = parseHtmToPageEntity(url,siteEntity,doc);
            if (!indexingProcessing.get()) {
                stopIndexing();
                return ;
            }
            siteEntity.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
            siteRepository.save(siteEntity);
            saveLemmaAndPageToRepository(pageEntity);
            ArrayList<PageIndexing> parsers = new ArrayList<>();
            for (Element element : elements) {
                String link = element.absUrl("href");
                if (!indexingProcessing.get()) {
                    stopIndexing();
                    return ;
                }
                if (link.contains(siteEntity.getUrl()) && !link.contains("#") && !link.contains(".pdf") && !link.contains(".JPG") &&!link.contains(".jpg") && !lookedLinks.containsKey(link) && indexingProcessing.get()) {
                    forkIfLinkIsNew(link,pageEntity,parsers);
                }
            }
            for (PageIndexing pageIndexing : parsers) {
                if (!indexingProcessing.get()) {
                    stopIndexing();
                    return;
                }


            }
            setSiteIsIndexedAndSave();
        } catch (IOException | InterruptedException e) {
            e.fillInStackTrace();
        }
    }



    private void stopIndexing(){
        siteEntity.setStatusType(Status.FAILED);
        siteEntity.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
        siteEntity.setLastError("«Индексация остановлена пользователем");
        siteRepository.save(siteEntity);
    }
    private PageEntity parseHtmToPageEntity(String url,SiteEntity siteEntity,Document doc) throws IOException {
        Connection.Response response = Jsoup.connect(url).execute();
        PageEntity pageEntity = new PageEntity();
        pageEntity.setSiteId(siteEntity);
        pageEntity.setPath(url);
        pageEntity.setCode(response.statusCode());
        pageEntity.setContent(doc.toString());
        return pageEntity;
    }
    private void saveLemmaAndPageToRepository(PageEntity pageEntity) throws IOException, InterruptedException {
        ExampleMatcher exampleMatcher = ExampleMatcher.matching().withIgnorePaths("id");
        Example<PageEntity> example = Example.of(pageEntity, exampleMatcher);
        if (lemmaCounter.getPageRepository().exists(example)) {
            pageEntity = lemmaCounter.getPageRepository().findByPath(pageEntity.getPath()).get(0);
            lemmaCounter.getPageRepository().save(pageEntity);
            lemmaCounter.saveLemmaToRepository(pageEntity.getPath());
        } else {
            try {
                lemmaCounter.getPageRepository().save(pageEntity);
                lemmaCounter.saveLemmaToRepository(pageEntity.getPath());
            } catch (DataIntegrityViolationException e) {
                e.fillInStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void forkIfLinkIsNew (String link,PageEntity pageEntity,List<PageIndexing> parsers) {
        lookedLinks.put(link, false);
        PageIndexing pageIndexing = new PageIndexing(siteRepository, lookedLinks, link, level + 1, siteEntity,indexingProcessing, lemmaCounter);
        parsers.add(pageIndexing);
        pageIndexing.fork();
    }
    private void setSiteIsIndexedAndSave(){
        siteEntity.setStatusType(Status.INDEXED);
        siteEntity.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
        siteRepository.save(siteEntity);
    }


}