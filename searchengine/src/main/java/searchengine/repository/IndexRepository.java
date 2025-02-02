package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;

import java.util.List;

public interface IndexRepository extends JpaRepository<IndexEntity,Integer> {

    int countByPageId(PageEntity pageEntity);
    List<IndexEntity> findByLemmaId(LemmaEntity lemmaEntity);

    List<IndexEntity> findByPageId(PageEntity pageEntity);

}
