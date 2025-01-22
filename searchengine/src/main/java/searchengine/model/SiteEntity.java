package searchengine.model;

import com.sun.istack.NotNull;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Getter
@Setter
@Entity
@Table(name = "site")
public class SiteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private int id;

    @Column(name="status")
    @NotNull
    private Status statusType;

    @Column(name="status_time", columnDefinition = "DATETIME")
    @NotNull
    private Timestamp statusTime;

    @Column(name="last_error", columnDefinition = "TEXT")
    private String lastError;
    @Column(name="url", columnDefinition = "VARCHAR(255)")
    @NotNull
    private String url;

    @Column(name = "name", columnDefinition = "VARCHAR(255)")
    @NotNull
    private String name;

    @OneToMany
    @JoinColumn(name="site_id", referencedColumnName = "id")
    private List<PageEntity> pageList = new ArrayList<>();


    @OneToMany
    @JoinColumn(name="site_id", referencedColumnName = "id")
    private List<LemmaEntity> lemmaList = new ArrayList<>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Status getStatusType() {
        return statusType;
    }

    public void setStatusType(Status statusType) {
        this.statusType = statusType;
    }



    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<PageEntity> getPageList() {
        return pageList;
    }

    public Timestamp getStatusTime() {
        return statusTime;
    }

    public void setStatusTime(Timestamp statusTime) {
        this.statusTime = statusTime;
    }

    public List<LemmaEntity> getLemmaList() {
        return lemmaList;
    }

    public void setLemmaList(List<LemmaEntity> lemmaList) {
        this.lemmaList = lemmaList;
    }

    public void setPageList(List<PageEntity> pageList) {
        this.pageList = pageList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SiteEntity that = (SiteEntity) o;
        return id == that.id && statusType == that.statusType && Objects.equals(statusTime, that.statusTime) && Objects.equals(lastError, that.lastError) && Objects.equals(url, that.url) && Objects.equals(name, that.name) && Objects.equals(pageList, that.pageList) && Objects.equals(lemmaList, that.lemmaList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, statusType, statusTime, lastError, url, name, pageList, lemmaList);
    }
}
