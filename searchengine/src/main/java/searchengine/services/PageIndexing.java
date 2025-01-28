package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.orm.jpa.JpaSystemException;
import searchengine.config.Site;
import searchengine.model.IndexEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;


import java.io.IOException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.sleep;

public class PageIndexing extends RecursiveTask<ArrayList<PageEntity>> {
    private final SiteRepository siteRepository;


    private final ConcurrentHashMap<String, Boolean> lookedLinks;

    private final String url;
    private final String mainUrl;

    private final int level;


    private final SiteEntity siteEntity;

    private final AtomicBoolean indexingProcessing;

    private final LemmaCounter lemmaCounter;

    public PageIndexing(SiteRepository siteRepository, ConcurrentHashMap<String, Boolean> lookedLinks, String url, String mainUrl, int level, SiteEntity siteEntity, AtomicBoolean indexingProcessing, LemmaCounter lemmaCounter) {
        this.siteRepository = siteRepository;
        this.lookedLinks = lookedLinks;
        this.url = url;
        this.mainUrl = mainUrl;
        this.level = level;
        this.siteEntity = siteEntity;
        this.lemmaCounter = lemmaCounter;
        this.indexingProcessing = indexingProcessing;


    }

    @Override
    protected ArrayList<PageEntity> compute() {


        ArrayList<PageEntity> resultLinks = new ArrayList<>();

        if (!indexingProcessing.get()) {
            return resultLinks;
        }

        try {


            Document doc = Jsoup.connect(url).timeout(100000).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").referrer("http://www.google.com").get();

            Elements elements = doc.select("a[href]");
            Connection.Response response = Jsoup.connect(url).execute();


            if (!indexingProcessing.get()) {
                return resultLinks;
            }

            PageEntity pageEntity = new PageEntity();

            pageEntity.setSiteId(siteEntity);
            pageEntity.setPath(url);
            pageEntity.setCode(response.statusCode());
            pageEntity.setContent(doc.toString());


            ExampleMatcher exampleMatcher = ExampleMatcher.matching().withIgnorePaths("id");
            Example<PageEntity> example = Example.of(pageEntity, exampleMatcher);

            if (lemmaCounter.getPageRepository().exists(example)) {
                pageEntity = lemmaCounter.getPageRepository().findByPath(pageEntity.getPath()).get(0);
                lemmaCounter.getPageRepository().save(pageEntity);
                //lemmaCounter.saveLemmaToRepository(pageEntity.getPath());
            } else {
                try {
                    lemmaCounter.getPageRepository().save(pageEntity);
                    lemmaCounter.saveLemmaToRepository(pageEntity.getPath());

                } catch (DataIntegrityViolationException e) {

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            siteEntity.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
            siteRepository.save(siteEntity);

            ArrayList<PageIndexing> parsers = new ArrayList<>();

            for (Element element : elements) {

                String link = element.absUrl("href");

                if (link.contains(mainUrl) && !link.contains("#") && !link.contains(".pdf") && !link.contains(".jpg")) {

                    if (!lookedLinks.containsKey(link) && indexingProcessing.get()) {
                        lookedLinks.put(link, false);


                        resultLinks.add(pageEntity);
                        PageIndexing pageIndexing = new PageIndexing(siteRepository, lookedLinks, link, this.mainUrl, level + 1, siteEntity,/*pageEntity,*/indexingProcessing, lemmaCounter);
                        parsers.add(pageIndexing);
                        pageIndexing.fork();
                    } else if (!indexingProcessing.get()) {
                        return resultLinks;
                    }


                }
            }


            for (PageIndexing pageIndexing : parsers) {
                if (!indexingProcessing.get()) {
                    return resultLinks;
                }
                ArrayList<PageEntity> childLinks = pageIndexing.join();//pageIndexing.compute();
                for (PageEntity child : childLinks) {
                    if (!indexingProcessing.get()) {
                        return resultLinks;
                    }
                    resultLinks.add(child);
                }
            }

        } catch (IOException | JpaSystemException | DataIntegrityViolationException e) {

            return resultLinks;
        }

        return resultLinks;

    }

}
