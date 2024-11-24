package searchengine.model;

import lombok.Data;
import javax.persistence.*;
import org.hibernate.annotations.Index;

@Data
@Entity
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(columnDefinition = "TEXT", nullable = false)
    @Index(name = "idx_path") //проиндексировано
    private String path;

    @Column(name = "code", nullable = false)
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

//    id INT NOT NULL AUTO_INCREMENT;
//    site_id INT NOT NULL — ID веб-сайта из таблицы site;
//    path TEXT NOT NULL — адрес страницы от корня сайта (должен начинаться со слэша, например: /news/372189/);
//    code INT NOT NULL — код HTTP-ответа, полученный при запросе страницы (например, 200, 404, 500 или другие);
//    content MEDIUMTEXT NOT NULL — контент страницы (HTML-код).

}
