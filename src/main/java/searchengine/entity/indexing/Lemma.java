package searchengine.entity.indexing;

import lombok.Data;
import lombok.NoArgsConstructor;
import searchengine.entity.SitePage;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
public class Lemma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT", nullable = false)
    private int id;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private SitePage sitePage;

    @Column(columnDefinition = "VARCHAR(255)")
    private String lemma;

    @Column(name = "frequency", nullable = false)
    private int frequency;

    @OneToMany(mappedBy = "lemma", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Index> indexes = new ArrayList<>();

//    @Override
//    public boolean equals(Object obj) {
//        Lemma l = (Lemma) obj;
//        return lemma.equals(l.lemma) && site == l.site;
//    }
//
//    @Override
//    public int hashCode() {
//        return lemma.hashCode() + site.hashCode();
//    }
//
//    @Override
//    public String toString() {
//        return "id: " + id + "; lemma: " + lemma + "; frequency: " + frequency + "; site: " + site.getName();
//    }


}
