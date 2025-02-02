package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class LemmaCounter {

    private final PageRepository pageRepository;

    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    private final LuceneMorphology luceneMorphology;


    public LemmaCounter(PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository) throws IOException {
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        luceneMorphology = new RussianLuceneMorphology();
    }

    public void saveLemmaToRepository(String url) throws IOException, InterruptedException {
        Map<String, Integer> lemmaInPage = lemmaCount(url);
        List<PageEntity> pageEntityList = pageRepository.findByPath(url);

        for (Map.Entry<String, Integer> entry : lemmaInPage.entrySet()) {
            int frequency = entry.getValue();
            String lemma = entry.getKey();
            Integer lemmaCountInPage = entry.getValue();
            IndexEntity indexEntity = new IndexEntity();
            LemmaEntity lemmaEntity = new LemmaEntity();


            PageEntity pageEntity = pageEntityList.get(0);

            List<LemmaEntity> lemmaInRepository = lemmaRepository.findByLemma(lemma);
            if (lemmaInRepository.isEmpty()) {


                lemmaEntity.setLemma(lemma);
                lemmaEntity.setSiteId(pageEntity);
                lemmaEntity.setFrequency(frequency);

                indexEntity.setLemmaId(lemmaEntity);
                indexEntity.setPageId(pageEntity);
                indexEntity.setRank((float) lemmaCountInPage);

                ExampleMatcher exampleMatcher = ExampleMatcher.matching().withIgnorePaths("id");
                Example<IndexEntity> example = Example.of(indexEntity, exampleMatcher);
                if (!indexRepository.exists(example)) {
                    lemmaRepository.save(lemmaEntity);
                    indexRepository.save(indexEntity);
                } else {
                    lemmaRepository.save(lemmaEntity);
                }

            } else if (!(lemmaInRepository.get(0).getSiteId().getId() == pageEntity.getId())) {
                lemmaEntity.setLemma(lemma);
                lemmaEntity.setSiteId(pageEntity);
                lemmaEntity.setFrequency(frequency);
                lemmaRepository.save(lemmaEntity);


                indexEntity.setLemmaId(lemmaEntity);
                indexEntity.setPageId(pageEntity);
                indexEntity.setRank((float) lemmaCountInPage);

                ExampleMatcher exampleMatcher = ExampleMatcher.matching().withIgnorePaths("id");
                Example<IndexEntity> example = Example.of(indexEntity, exampleMatcher);

                if (!indexRepository.exists(example)) {
                    indexRepository.save(indexEntity);

                }


            }


        }
    }

    public Map<String, Integer> lemmaCount(String url) throws IOException, InterruptedException {
        Map<String, Integer> lemmas = new HashMap<>();
        String text = parseHtmlToString(url);
        List<String> words = saveOnlyLemmas(text);

        List<Thread> lemmaCountList = new ArrayList<>();
        for (String word : words) {
            Runnable lemmaCounting = () -> {

                List<String> normalForm = luceneMorphology.getNormalForms(word);
                if (!lemmas.containsKey(normalForm.get(0))) {
                    lemmas.put(normalForm.get(0), 1);
                } else {
                    lemmas.replace(normalForm.get(0), lemmas.get(normalForm.get(0)), lemmas.get(normalForm.get(0)) + 1);
                }


            };
            Thread thread = new Thread(lemmaCounting);
            lemmaCountList.add(thread);
            thread.start();


        }
        for (Thread thread : lemmaCountList) {
            thread.join();
        }
        return lemmas;

    }

    public List<String> saveOnlyLemmas(String text) throws IOException {
        List<String> lemmas = new ArrayList<>();
        String[] words = text.split("\\s+");
        List<String> textList = new ArrayList<>();
        String regex = "[^а-яА-я]";

        for (int i = 0; i < words.length; i++) {
            words[i] = words[i].replaceAll(regex, "");
            words[i] = words[i].toLowerCase();
            if (!words[i].isEmpty()) {
                lemmas.addAll(luceneMorphology.getMorphInfo(words[i]));

            } else {
                continue;
            }
            String lemma = lemmas.get(0);
            if ((!lemma.contains("МЕЖД")) && (!lemma.contains("СОЮЗ")) && (!lemma.contains("ПРЕДЛ"))) {


                textList.add(luceneMorphology.getNormalForms(words[i]).get(0));

            }
            lemmas.clear();

        }
        return textList;
    }

    private String parseHtmlToString(String url) throws IOException {
        return pageRepository.findByPath(url).get(0).getContent();

    }


    public PageRepository getPageRepository() {
        return pageRepository;
    }

    public LemmaRepository getLemmaRepository() {
        return lemmaRepository;
    }

    public IndexRepository getIndexRepository() {
        return indexRepository;
    }
}
