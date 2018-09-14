import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Ricardo on 5/9/2018.
 */
public class Crawler implements Runnable {
    private WebPage page;
    private URLFrontierAdmin urlFrontierAdmin;
    private final String TEMP_FILE = "temp";

    public Crawler(WebPage page, URLFrontierAdmin urlFrontierAdmin)
    {
        this.page = page;
        this.urlFrontierAdmin = urlFrontierAdmin;
    }

    public void run() {
        InetAddress ip = InetAddress.getLoopbackAddress();
        URL url = page.getURL();
        Set<String> links;
        //Resolve DNS
        try {
            ip = resolveDNS(url);
        } catch (UnknownHostException e) {
            // Host inválido
            return;
        }
        //Fetch Page
        try {
            fetchPage(url);
        } catch (IOException e) {
            // No pudo copiar archivo
            return;
        }
        //Parse Page
        try {
            links = parsePage();
        } catch (IOException e) {
            //No pudo leer archivo
            return;
        }
        //Add links to URL frontier
        for(String link : links) {
            try {
                URL linkURL = linkToURL(link);
                //Agregar link a URL frontier
                WebPage linkPage = urlFrontierAdmin.find(linkURL);

                if(linkPage == null) { //link not in URL frontier
                    linkPage = new WebPage(linkURL);
                    urlFrontierAdmin.addPage(linkPage);
                }

                linkPage.addIncomingLink(page);
            } catch (MalformedURLException e) {
                urlFrontierAdmin.addToErrorList(link);
                links.remove(link);
            }
        }
        //Actualizar links en WebPage actual
        page.setOutgoingLinks(links.size());
    }

    /**
     * Determina el servidor web del cuál recuperar la página
     * @param url URL de la página
     * @return Dirección IP del servidor web
     */
    private InetAddress resolveDNS(URL url) throws UnknownHostException{
        return InetAddress.getByName(url.getHost());
    }

    /**
     * Recupera la página en un URL
     * @param url URL de la página
     */
    private void fetchPage(URL url) throws IOException {
        InputStream in = url.openStream();
        Files.copy(in, Paths.get(TEMP_FILE));
    }

    /**
     * Extrae el texto y el conjunto de enlaces de una página
     * @return Lista de links en la página, si es html
     */
    private Set<String> parsePage() throws IOException {
        ///TODO agregar sólo .html
        FileReader reader = new FileReader(TEMP_FILE);
        HashSet<String> linkSet = new HashSet<>();
        ParserDelegator parserDelegator = new ParserDelegator();
        HTMLEditorKit.ParserCallback parserCallback = new HTMLEditorKit.ParserCallback() {
            public void handleStartTag(HTML.Tag tag, MutableAttributeSet attribute, int pos) {
                if (tag == HTML.Tag.A) {
                    String address = (String) attribute.getAttribute(HTML.Attribute.HREF);
                    linkSet.add(address);
                }
            }
        };
        parserDelegator.parse(reader, parserCallback, true);
        return linkSet;
    }

    /**
     * Determina si un link extraído ya está en el URL frontier
     * @param link Link extraído
     * @return true si el link está en el URL frontier
     */
    private URL linkToURL(String link) throws MalformedURLException {
        if(urlFrontierAdmin.isInErrorList(link)) {
            return null;
        } else {
            return new URL(link);
        }
    }
}
