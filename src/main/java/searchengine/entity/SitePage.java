package searchengine.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import searchengine.entity.indexing.Lemma;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.*;

@Data
@Entity
@NoArgsConstructor
public class SitePage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT", nullable = false)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')", nullable = false)
    private Status status;

    @Column(columnDefinition = "DATETIME", nullable = false)
    private Date statusTime;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false, unique = true)
    private String url;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;

    @OneToMany(mappedBy = "sitePage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Page> pages = new ArrayList<>();

    @OneToMany(mappedBy = "sitePage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lemma> lemmas = new ArrayList<>();

//    @Override
//    public String toString() {
//        return "id: " + id + ", url: " + url + ", name: " + name;
//    }
//
//    @Override
//    public int hashCode() {
//        return url.hashCode();
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//        return obj.getClass() == Site.class;
//    }

}
