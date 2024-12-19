package searchengine.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import searchengine.entity.indexing.Index;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;


@Data
@Entity
//@Table(name = "Page", indexes = {
//        @javax.persistence.Index(name = "idx_page_path", columnList = "path")
//})
@NoArgsConstructor
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT", nullable = false)
    private int id;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private SitePage sitePage;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String path;

    @Column(name = "code", nullable = false)
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Index> indexList = new ArrayList<>();

//    @Override
//    public int hashCode() {
//        return path != null && site != null ? path.hashCode() + site.hashCode() : 0;
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//        Page p = (Page) obj;
//        return site == null ||
//                getClass() == obj.getClass() && path.equals(p.path) && site == p.site;
//    }
//
//    @Override
//    public String toString() {
//        return "id: " + id + ", siteId: " + site.getId() + ", path: " + path;
//    }

}
