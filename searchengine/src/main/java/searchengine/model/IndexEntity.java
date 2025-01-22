package searchengine.model;

import com.sun.istack.NotNull;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Objects;


@Getter
@Setter
@Entity
@Table(name = "indexes")
public class IndexEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;


    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "page_id")
    private PageEntity pageId;


    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "lemma_id")
    private LemmaEntity lemmaId;
    @Column(name="rankes", columnDefinition = "FLOAT")
    @NotNull
    private Float rank;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public PageEntity getPageId() {
        return pageId;
    }

    public void setPageId(PageEntity pageId) {
        this.pageId = pageId;
    }

    public LemmaEntity getLemmaId() {
        return lemmaId;
    }

    public void setLemmaId(LemmaEntity lemmaId) {
        this.lemmaId = lemmaId;
    }

    public Float getRank() {
        return rank;
    }

    public void setRank(Float rank) {
        this.rank = rank;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexEntity that = (IndexEntity) o;
        return Objects.equals(pageId, that.pageId) && Objects.equals(lemmaId, that.lemmaId) && Objects.equals(rank, that.rank);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageId, lemmaId, rank);
    }
}
