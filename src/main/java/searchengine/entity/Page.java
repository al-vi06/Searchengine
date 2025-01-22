package searchengine.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import searchengine.entity.indexing.Index;
import searchengine.entity.indexing.Lemma;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT", nullable = false)
    private int id;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false, insertable = false, updatable = false)
    private SitePage site;

//    @Column(name = "site_id", nullable = false)
//    private int siteId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String path;

    @Column(name = "code", nullable = false)
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    //связь с Index
    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Index> index = new ArrayList<>();


}
