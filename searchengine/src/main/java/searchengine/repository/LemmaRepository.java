package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.List;

public interface LemmaRepository extends JpaRepository<LemmaEntity,Integer> {
    List<LemmaEntity> findByLemma(String lemma);

//     @Query("SELECT COUNT(*) FROM lemma WHERE siteid=?1")
//     long getCountOfSiteId(@Param("siteid") int siteId);

    //@Query("SELECT u FROM lemma u WHERE u.siteid=?1")
    List<LemmaEntity> findBySiteId(SiteEntity siteEntity);
    int countBySiteId(SiteEntity siteEntity);

}
