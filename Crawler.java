/**
 * Created by Ricardo on 5/9/2018.
 */
public class Crawler implements Runnable {
    private WebPage page;
    private URLFrontierAdmin urlFrontierAdmin;

    public Crawler(WebPage page, URLFrontierAdmin urlFrontierAdmin)
    {
        this.page = page;
        this.urlFrontierAdmin = urlFrontierAdmin;
    }

    public void run() {

    }
}
