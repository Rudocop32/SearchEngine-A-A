package searchengine.dto.statistics;
public class PageData {

    private final String site;

    private final String siteName;
    private final String uri;

    private final String title;
    private final String snippet;

    private final double relevance;

    public PageData(String site, String siteName, String uri, String title, String snippet, double relevance) {
        this.site = site;
        this.siteName = siteName;
        this.uri = uri;
        this.title = title;
        this.snippet = snippet;
        this.relevance = relevance;
    }

    public String getSite() {
        return site;
    }

    public String getSiteName() {
        return siteName;
    }

    public String getUri() {
        return uri;
    }

    public String getTitle() {
        return title;
    }

    public String getSnippet() {
        return snippet;
    }

    public double getRelevance() {
        return relevance;
    }

}
