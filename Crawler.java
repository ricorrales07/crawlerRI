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
import java.util.ArrayList;
import java.util.List;

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
        try {
            ip = resolveDNS(url);
        } catch (UnknownHostException e) {
            // Host inválido
            return;
        }

        try {
            fetchPage(url);
        } catch (IOException e) {
            // No pudo leer archivo
            return;
        }
    }

    /**
     * Determina el servidor web del cuál recuperar la página
     */
    private InetAddress resolveDNS(URL url) throws UnknownHostException{
        return InetAddress.getByName(url.getHost());
    }

    /**
     * Recupera la página en un URL
     */
    private void fetchPage(URL url) throws IOException {
        InputStream in = url.openStream();
        Files.copy(in, Paths.get(TEMP_FILE));
    }

    /**
     * Extrae el texto y el conjunto de enlaces de una página
     */
    private List<String> parsePage() throws IOException {
        ///TODO agregar sólo .html
        FileReader reader = new FileReader(TEMP_FILE);
        ArrayList<String> linkList = new ArrayList<>();
        ParserDelegator parserDelegator = new ParserDelegator();
        HTMLEditorKit.ParserCallback parserCallback = new HTMLEditorKit.ParserCallback() {
            public void handleStartTag(HTML.Tag tag, MutableAttributeSet attribute, int pos) {
                if (tag == HTML.Tag.A) {
                    String address = (String) attribute.getAttribute(HTML.Attribute.HREF);
                    linkList.add(address);
                }
            }
        };
        parserDelegator.parse(reader, parserCallback, true);
        return linkList;
    }

    /**
     * Determina si un link extraído ya está en el URL frontier
     * @param link
     * @return
     */
    private boolean eliminateDup(String link) throws MalformedURLException {
        URL url = new URL(link);
        return urlFrontierAdmin.contains(url);
    }
}
