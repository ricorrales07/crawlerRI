import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ricardo on 6/9/2018.
 */
public class WebPage {
    /**
     * Estamos usando la fórmula de Page Rank que usaba Google originalmente.
     * Más detalles aquí: http://infolab.stanford.edu/~backrub/google.html
     */
    final static double dampingFactor = 0.85;

    private URL url;
    private List<WebPage> incomingLinks;
    private double ranking;
    private int outgoingLinks;

    public WebPage(String url) throws MalformedURLException {
        this.url = new URL(url);
        incomingLinks = new ArrayList<WebPage>();
        ranking = 1;
        outgoingLinks = 0;
    }

    public WebPage(URL url) {
        this.url = url;
        incomingLinks = new ArrayList<WebPage>();
        ranking = 1;
        outgoingLinks = 0;
    }

    public URL getURL() {
        return url;
    }

    public void setOutgoingLinks(int outgoingLinks) {
        this.outgoingLinks = outgoingLinks;
    }

    private int getOutgoingLinks() {
        return outgoingLinks;
    }

    public double getRanking() {
        return ranking;
    }

    // OJO: Asegurarse de haber contado primero los outgoing links de
    // "source" antes de enviarla a este método.
    public void addIncomingLink(WebPage source) throws IllegalArgumentException {
        if(source.getOutgoingLinks() == 0)
            throw new IllegalArgumentException("Terminar de contar los " +
                "outgoing links de la página antes de agregarla como " +
                "incoming link de otra.");
        incomingLinks.add(source);
        updateRanking();
    }

    // Privada por default. Aún no sé si se necesita en otra parte.
    // No estoy tomando en cuenta frecuencia de actualización. No estoy muy seguro de cómo hacerlo.
    private void updateRanking() {
        double sum = 0;
        for (WebPage p : incomingLinks) {
                sum += p.getRanking() / p.getOutgoingLinks();
        }
        ranking = (1-dampingFactor) + dampingFactor * sum;
    }

    public boolean equals(Object o) {
        if (o == null)
            return false;
        else if (o instanceof URL)
            return this.url.equals(o);
        else if (o instanceof WebPage)
            return this.url.equals(((WebPage) o).url);
        else
            return false;
    }
}
