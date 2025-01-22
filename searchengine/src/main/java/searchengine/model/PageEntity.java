package searchengine.model;

import com.sun.istack.NotNull;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.*;

@Getter
@Setter
@Entity
@Table(name = "page", indexes = {@Index(name = "path_index", columnList = "path")})
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @ManyToOne
    @JoinColumn(name = "site_id")
    private SiteEntity siteId;


    @Column(name = "path",unique = true)
    private String path;

    @Column(name = "code")
    @NotNull
    private int code;

    @Column(name = "content",columnDefinition = "MEDIUMTEXT")
    private String content;



    @OneToMany
    @JoinColumn(name = "page_id",referencedColumnName = "id")
    private List<IndexEntity> indexEntityList = new ArrayList<>();


    public List<IndexEntity> getIndexEntityList() {
        return indexEntityList;
    }

    public void setIndexEntityList(List<IndexEntity> indexEntityList) {
        this.indexEntityList = indexEntityList;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public SiteEntity getSiteId() {
        return siteId;
    }

    public void setSiteId(SiteEntity siteId) {
        this.siteId = siteId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageEntity that = (PageEntity) o;
        return id == that.id && code == that.code && Objects.equals(siteId, that.siteId) && Objects.equals(path, that.path) && Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, siteId, path, code, content);
    }

    @Override
    public String toString() {
        return "PageEntity{" +
                "id=" + id +
                ", siteId=" + siteId +
                ", path='" + path + '\'' +
                ", code=" + code +
                '}';
    }

}
