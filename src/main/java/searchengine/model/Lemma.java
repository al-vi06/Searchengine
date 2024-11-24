package searchengine.model;

import org.springframework.data.annotation.Id;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;

public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "site_id", nullable = false)
    private int siteId;
    @Column(columnDefinition = "VARCHAR(255)")
    private String lemma;
    @Column(name = "frequency", nullable = false)
    private int frequency;

//    id INT NOT NULL AUTO_INCREMENT;
//    site_id INT NOT NULL — ID веб-сайта из таблицы site;
//    lemma VARCHAR(255) NOT NULL — нормальная форма слова (лемма);
//    frequency INT NOT NULL — количество страниц, на которых слово встречается хотя бы один раз. Максимальное значение не может превышать общее количество слов на сайте.

}
