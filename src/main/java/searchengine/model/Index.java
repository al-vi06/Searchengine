package searchengine.model;

import org.springframework.data.annotation.Id;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;

public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "page_id", nullable = false)
    private int pageId;
    @Column(name = "lemma_id", nullable = false)
    private int lemmaId;
    @Column(columnDefinition = "FLOAT")
    private double rank;

//    id INT NOT NULL AUTO_INCREMENT;
//    page_id INT NOT NULL — идентификатор страницы;
//    lemma_id INT NOT NULL — идентификатор леммы;
//    rank FLOAT NOT NULL — количество данной леммы для данной страницы.

}
