package searchengine.services;

import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
//@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {





    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    public StatisticsServiceImpl(SitesList sites, SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository) {
        this.sites = sites;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
    }

    @Override
    public StatisticsResponse getStatistics() {
       List<SiteEntity> siteEntityList = siteRepository.findAll();
       if(siteEntityList.isEmpty()){
           return getStartStatistics();
       }


       TotalStatistics totalStatistics = new TotalStatistics();
       totalStatistics.setSites(sites.getSites().size());
       totalStatistics.setIndexing(true);

       List<DetailedStatisticsItem> detailed = new ArrayList<>();

       for(SiteEntity siteEntity : siteEntityList){
           Site site = new Site();
           site.setName(siteEntity.getName());
           site.setUrl(siteEntity.getUrl());

           DetailedStatisticsItem item = new DetailedStatisticsItem();
           item.setName(site.getName());
           item.setUrl(site.getUrl());

           int pages = pageRepository.countBySiteId(siteEntity);
           int lemmas = lemmaRepository.countBySiteId(siteEntity);
           item.setPages(pages);
           item.setLemmas(lemmas);
           item.setStatus(siteEntity.getStatusType().toString());
           item.setStatusTime( siteEntity.getStatusTime().getTime());

           totalStatistics.setPages(totalStatistics.getPages() + pages);
           totalStatistics.setLemmas(totalStatistics.getLemmas() + lemmas);

           detailed.add(item);
       }
       StatisticsResponse response = new StatisticsResponse();
       StatisticsData data = new StatisticsData();
       data.setTotal(totalStatistics);
       data.setDetailed(detailed);

       response.setStatistics(data);
       response.setResult(true);
       return response;
    }
    StatisticsResponse getStartStatistics(){
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(false);
        List<DetailedStatisticsItem> detailed = new ArrayList<>();

        for(Site site : sites.getSites()){

            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            item.setPages(0);
            item.setLemmas(0);
            item.setStatus(null);
            item.setError(null);
            item.setStatus("WAIT");
            item.setStatusTime(Instant.now().toEpochMilli());

            detailed.add(item);
        }
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
