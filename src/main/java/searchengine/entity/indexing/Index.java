package searchengine.entity.indexing;

import lombok.Data;
import searchengine.entity.Page;
import javax.persistence.*;


@Data
@Entity
@Table(name = "`index`")
public class Index {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT", nullable = false)
    private int id;

    @ManyToOne
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;

    @ManyToOne
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma;

    @Column(name = "`rank`", columnDefinition = "FLOAT")
    private double rank;

//    @Override
//    public boolean equals(Object obj) {
//        if (obj.getClass() != getClass()) {
//            return false;
//        }
//        Index i = (Index) obj;
//        return id == i.id;
//    }
//
//    @Override
//    public int hashCode() {
//        return id + page.hashCode() + lemma.hashCode();
//    }
//
//    @Override
//    public String toString() {
//        return "id: " + id + "; page: " + page.getPath() + "; lemma: " + lemma.getLemma();
//    }

}
