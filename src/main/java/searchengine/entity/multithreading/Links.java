package searchengine.entity.multithreading;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Links {
    private final String url;
    private String content;
    private List<Links> childLinks = new ArrayList<>();

//    public Links(String url) {
//        this.url = url;
//        childLinks = new ArrayList<>();
//    }

//    public String getUrl() {
//        return url;
//    }
//
//    public List<Links> getChildLinks() {
//        return childLinks;
//    }
//
//    public void setChildLinks(List<Links> childLinks) {
//        this.childLinks = childLinks;
//    }
   public void addChildLink(Links link) {
        childLinks.add(link);
    }

}
