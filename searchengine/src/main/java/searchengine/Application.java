package searchengine;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import searchengine.services.LemmaCounter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SpringBootApplication
public class Application {
    public static void main(String[] args) throws IOException {
        SpringApplication.run(Application.class, args);


//        String text = "Повторное появление леопарда в Осетии позволяет предположить, что леопард постоянно обитает в некоторых районах Северного Кавказа.";
//
//        LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
//
//        LemmaCounter lemmaCounter = new LemmaCounter();
//
//       Map<String,Integer> lemma = lemmaCounter.lemmaCount("https://kuzovsibir.ru/");
//        for (Map.Entry<String, Integer> entry : lemma.entrySet()) {
//            String key = entry.getKey();
//            Integer value = entry.getValue();
//            System.out.println("Key: " + key + ", Value: " + value);
//        }

        }
    }



