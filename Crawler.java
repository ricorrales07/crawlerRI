/**
 * Created by Ricardo on 5/9/2018.
 */
public class Crawler implements Runnable {
    private String m_url;
    private URLFrontierAdmin m_urlFrontierAdmin;

    public Crawler(String url, URLFrontierAdmin urlFrontierAdmin)
    {
        m_url = url;
        m_urlFrontierAdmin = urlFrontierAdmin;
    }

    public void run() {

    }
}
