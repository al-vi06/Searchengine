package searchengine.entity;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import searchengine.entity.indexing.Lemma;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.*;


@Entity
@Table(name = "site")
@Setter @Getter
@NoArgsConstructor
public class SitePage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT", nullable = false)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false) //columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')",
    private Status status;

    @Column(columnDefinition = "DATETIME", nullable = false)
    private Date statusTime;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false, unique = true)
    private String url;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;

    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Page> pages = new ArrayList<>();

    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lemma> lemmas = new ArrayList<>();

}
