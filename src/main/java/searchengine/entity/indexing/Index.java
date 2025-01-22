package searchengine.entity.indexing;

import com.sun.istack.NotNull;
import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
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
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "page_id", insertable = false, updatable = false, nullable = false)
    private Page page;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "lemma_id", insertable = false, updatable = false, nullable = false)
    private Lemma lemma;

    @Column(name = "rank", columnDefinition = "FLOAT", nullable = false)//`rank`
    private float rank;


}
