package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.List;

public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    List<PageEntity> findByPath(String path);


    List<PageEntity> findBySiteId(SiteEntity siteEntity);

    int countBySiteId(SiteEntity siteEntity);
}
