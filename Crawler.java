/**
 * Created by Ricardo on 5/9/2018.
 */
public class Crawler implements Runnable {
    private String url;
    private URLFrontierAdmin urlFrontierAdmin;

    public Crawler(String url, URLFrontierAdmin urlFrontierAdmin)
    {
        this.url = url;
        this.urlFrontierAdmin = urlFrontierAdmin;
    }

    public void run() {

    }
}
