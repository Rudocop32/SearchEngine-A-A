package searchengine.model;

import com.sun.istack.NotNull;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.*;

@Getter
@Setter
@Entity
@Table(name = "lemma")
public class LemmaEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;


    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "site_id",updatable = false)
    private SiteEntity siteId;
    @Column(name = "lemma", columnDefinition = "VARCHAR(255)")
    @NotNull
    private String lemma;


    @Column(name = "frequency")
    @NotNull
    private int frequency;


    @OneToMany
    @JoinColumn(name = "lemma_id", referencedColumnName = "id")
    List<IndexEntity> indexEntityList = new ArrayList<>();


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




    public String getLemma() {
        return lemma;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public SiteEntity getSiteId() {
        return siteId;
    }

    public void setSiteId(SiteEntity siteId) {
        this.siteId = siteId;
    }
}
