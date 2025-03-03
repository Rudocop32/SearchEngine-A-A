package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.PageData;
import searchengine.response.FalseResponse;
import searchengine.response.PageResponseTrue;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.*;

@Service
public class SearchService {
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;

    private final SiteRepository siteRepository;

    private final LemmaCounter lemmaCounter;

    private final PageResponseTrue pageResponseTrue;
    private final LuceneMorphology luceneMorphology;

    public SearchService(LemmaRepository lemmaRepository, PageRepository pageRepository, IndexRepository indexRepository, SiteRepository siteRepository) throws IOException {
        this.lemmaRepository = lemmaRepository;
        this.pageRepository = pageRepository;
        this.indexRepository = indexRepository;
        this.siteRepository = siteRepository;
        lemmaCounter = new LemmaCounter(pageRepository, lemmaRepository, indexRepository);
        pageResponseTrue = new PageResponseTrue();
        luceneMorphology = new RussianLuceneMorphology();
    }
    public ResponseEntity<Object> search(String query, String site, int offset, int limit) throws IOException {
        if(offset >0){
            offset/=limit;
        }
        Map<String, Integer> sortedLemma = sortLemmaByFrequency(query);
        List<PageData> pageDataList = findPagesFromOneLemma(sortedLemma, site);
        if(sortedLemma.size()>1){
            pageDataList = findPagesFromMoreLemma(sortedLemma,site);
        }
        List<PageData> result = new ArrayList<>();

        for (int i = limit * offset; i < limit * offset + limit; i++) {
            try {
                result.add(pageDataList.get(i));
            } catch (IndexOutOfBoundsException ex) {
                break;
            }
        }
        result = result.stream().sorted(Comparator.comparingDouble(PageData::getRelevance).reversed()).toList();
        if (result.isEmpty()) {
            String error = "Указанная страница не найдена";
            FalseResponse falseResponse = new FalseResponse(error);
            return ResponseEntity.ok(falseResponse);
        }
        pageResponseTrue.setResult(true);
        pageResponseTrue.setData(result);
        return ResponseEntity.ok(pageResponseTrue);
    }
    public Map<String, Integer> sortLemmaByFrequency(String inputText) throws IOException {
        List<String> lemmaList = lemmaCounter.saveOnlyLemmas(inputText);
        Map<String, Integer> sortedLemma = new HashMap<>();
        for (String lemma : lemmaList) {
            List<LemmaEntity> lemmaEntityList = lemmaRepository.findByLemma(lemma);
            if (!lemmaEntityList.isEmpty()) {
                LemmaEntity lemmaEntity = lemmaEntityList.get(0);
                PageEntity pageEntity = lemmaEntity.getSiteId();
                int frequency = lemmaEntity.getFrequency();
                if (isGoodLemma(frequency, lemmaRepository.countBySiteId(pageEntity))) {
                    sortedLemma.put(lemma, frequency);
                }
            }
        }
        return sortedLemma;
    }
    public List<PageData> findPagesFromOneLemma(Map<String, Integer> sortedLemma, String siteUrl) {
        List<PageEntity> pageEntityList = new ArrayList<>();
        List<PageData> pageDataList = new ArrayList<>();
        pageResponseTrue.setCount(0);
        for (Map.Entry<String, Integer> entry : sortedLemma.entrySet()) {
            String lemma = entry.getKey();
            List<LemmaEntity> lemmaEntityList = lemmaRepository.findByLemma(lemma);
            for (LemmaEntity lemmaEntity : lemmaEntityList) {
                List<IndexEntity> indexEntityList = indexRepository.findByLemmaId(lemmaEntity);
                pageResponseTrue.setCount(pageResponseTrue.getCount() + indexEntityList.size());
                    PageEntity pageEntity = lemmaEntity.getSiteId();
                    if (!(siteUrl == null)) {
                        if (!pageEntity.getSiteId().getUrl().equals(siteUrl)) {
                            continue;
                        }
                    }
                    if (!pageEntityList.contains(pageEntity)) {
                        pageDataList.add(createPageData(pageEntity,lemmaEntity,sortedLemma));
                        pageEntityList.add(pageEntity);
                    }
            }
        }
        return pageDataList;
    }
    public List<PageData> findPagesFromMoreLemma(Map<String, Integer> sortedLemma, String siteUrl) {
        List<PageEntity> pageEntityList = new ArrayList<>();
        List<PageData> pageDataList = new ArrayList<>();
        pageResponseTrue.setCount(0);
            List<LemmaEntity> lemmaEntityList =findTheLeastLemmas(sortedLemma);
            for (LemmaEntity lemmaEntity : lemmaEntityList) {
                List<IndexEntity> indexEntityList = indexRepository.findByLemmaId(lemmaEntity);
                pageResponseTrue.setCount(pageResponseTrue.getCount() + indexEntityList.size());
                PageEntity pageEntity = lemmaEntity.getSiteId();
                if (!(siteUrl == null)) {
                    if (!pageEntity.getSiteId().getUrl().equals(siteUrl)) {
                        continue;
                    }
                }
                if (!pageEntityList.contains(pageEntity)) {
                    pageDataList.add(createPageData(pageEntity,lemmaEntity,sortedLemma));
                    pageEntityList.add(pageEntity);
                }
            }

        return pageDataList;
    }


    public Integer getPageRelevance(PageEntity pageEntity) {
        List<IndexEntity> indexEntityList = indexRepository.findByPageId(pageEntity);
        int absRevelance = 0;
        for (IndexEntity indexEntity : indexEntityList) {
            absRevelance = absRevelance + Math.round(indexEntity.getRank());
        }
        return absRevelance;
    }
    public String generateSnippet(PageEntity pageEntity, LemmaEntity lemmaEntity, Map<String, Integer> sortedLemma) {
        String text = Jsoup.parse(pageEntity.getContent()).text();
        String resultText;
        StringBuilder stringBuilder = new StringBuilder();
        String lemma = lemmaEntity.getLemma();
        String[] words = text.split("\\s+");
        int lemmaNumber = findLemmaNumber(words,lemma);
        for (int i = Math.max(lemmaNumber - 5, 0); i < Math.min(lemmaNumber + 5, words.length); i++) {
            if (i == lemmaNumber) {
                stringBuilder.append("<b>").append(words[i]).append("</b>").append(" ");
                continue;
            }
            boolean wordIsLemma = false;
            try{
                if (sortedLemma.containsKey(makeAWordNormalForm(words[i]))){
                    stringBuilder.append("<b>" + words[i] + "</b>" + " ");
                    wordIsLemma = true;
                }
            }catch (Exception e){
                e.fillInStackTrace();
            }
            if (!wordIsLemma) {
                stringBuilder.append(words[i]).append(" ");
            }
        }
        resultText = stringBuilder.toString();
        return resultText;
    }
    private String getTittle(String url) throws IOException {
        String html = pageRepository.findByPath(url).get(0).getContent();
        Document doc = Jsoup.parse(html);
        Elements elements = doc.select("title");
        for (Element element : elements) {
            return element.text();

        }
        return elements.text();
    }
    private int findLemmaNumber(String[] words,String lemma){
        String regex = "[^а-яА-я]";
        int lemmaNumber = 0;
        for (int i = 0; i < words.length; i++) {
            String word = words[i].replaceAll(regex, "");
            word = word.toLowerCase();
            if (word.isEmpty()) {
                continue;
            }
            List<String> normalForm = luceneMorphology.getNormalForms(word);
            word = normalForm.get(0);
            if (word.equals(lemma)) {
                lemmaNumber = i;
                break;
            }
        }
        return lemmaNumber;
    }
    private String makeAWordNormalForm(String word){
        String regex = "[^а-яА-я]";
        String checkedWord = word.replaceAll(regex, "");
        checkedWord = checkedWord.toLowerCase();
        List<String> normalFormCheckedWord = luceneMorphology.getNormalForms(checkedWord);
        checkedWord = normalFormCheckedWord.get(0);
        return checkedWord;
    }
    private PageData createPageData(PageEntity pageEntity,LemmaEntity lemmaEntity,Map<String,Integer> sortedLemma){
        String tittle;
        try {
            tittle = getTittle(pageEntity.getPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String site = pageEntity.getSiteId().getUrl();
        String siteName = pageEntity.getSiteId().getName();
        String url = pageEntity.getPath().replace(site, "");
        String snippet = generateSnippet(pageEntity, lemmaEntity, sortedLemma);
        double relevance = (double) getPageRelevance(pageEntity);
        PageData pageData = new PageData(site, siteName, url, tittle, snippet, relevance);
        return pageData;
    }

    public boolean isGoodLemma(int frequency, int allLemmaInSite) {
        return (double) frequency / allLemmaInSite < 0.75;
    }
    public List<LemmaEntity> findTheLeastLemmas(Map<String, Integer> sortedLemma){
        List<LemmaEntity> lemmasWithSamePage = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : sortedLemma.entrySet()){
            String lemma = entry.getKey();
            List<LemmaEntity> lemmaEntityList = lemmaRepository.findByLemma(lemma);
            if(lemmasWithSamePage.isEmpty()){
                lemmasWithSamePage.addAll(lemmaEntityList);
            }
            if(lemmasWithSamePage.size() > lemmaEntityList.size()){
                lemmasWithSamePage.addAll(lemmaEntityList);
            }
        }
        return lemmasWithSamePage;
    }
}
