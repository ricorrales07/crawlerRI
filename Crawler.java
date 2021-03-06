import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.FileAlreadyExistsException;
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
    private static final Boolean lock = true;
    private static Integer pageNumber = 0;
    private static Integer robotsNumber = 0;

    public Crawler(WebPage page, URLFrontierAdmin urlFrontierAdmin)
    {
        this.page = page;
        this.urlFrontierAdmin = urlFrontierAdmin;
        System.out.println("New thread: " + page.getURL().toString());
    }

    public void run() {
        System.out.println("Thread running (" + page.getURL().toString() + ").");
        URL url = page.getURL();
        Set<String> links;
        //Resolve DNS
        try {
            InetAddress ip = resolveDNS(url);
            System.out.println("Resolved DNS for " + page.getURL().toString()
                + ": " + ip);
        } catch (UnknownHostException e) {
            // Host inválido
            System.out.println("Could not resolve DNS for " + page.getURL().toString());
            return;
        }
        //Check robots.txt
        try {
            if (!checkRobots(url)) {
                return;
            }
        } catch (IOException e) { }
        //Fetch Page
        String path = null;
        try {
            System.out.println("Fetching page " + page.getURL().toString());
            path = fetchPage(url);
            System.out.println("Page fetched: " + page.getURL().toString());
        } catch (IOException e) {
            // No pudo copiar archivo
            System.out.println(e.toString());
            return;
        }
        //Parse Page
        try {
            links = parsePage(url, path);
        } catch (IOException e) {
            //No pudo leer archivo
            System.out.println("Could not read file.");
            return;
        }

        synchronized (page) {
            page.setOutgoingLinks(links.size());
        }

        //Add links to URL frontier
        for(String link : links) {
            System.out.println("Found URL: " + link);
            if (link != null && link != "") {
                try {
                    URL linkURL = linkToURL(link);
                    //Agregar link a URL frontier
                    WebPage linkPage;
                    //urlFrontierAdmin.lock.lock();
                    linkPage = urlFrontierAdmin.find(linkURL);
                    //urlFrontierAdmin.lock.unlock();

                    if (linkPage == null) { //link not in URL frontier
                        linkPage = new WebPage(linkURL);
                        synchronized (page) {
                            linkPage.addIncomingLink(page);
                        }
                        //urlFrontierAdmin.lock.lock();
                        urlFrontierAdmin.addPage(linkPage);
                        //urlFrontierAdmin.lock.unlock();
                    }

                    /*synchronized (linkPage) {
                        linkPage.addIncomingLink(page);
                    }*/
                } catch (MalformedURLException e) {
                    urlFrontierAdmin.lock.lock();
                    urlFrontierAdmin.addToErrorList(link);
                    urlFrontierAdmin.lock.unlock();
                    //links.remove(link); // Esto estaba causando problemas.
                }
            }
        }
        //Actualizar links en WebPage actual
        /*synchronized (page) {
            page.setOutgoingLinks(links.size());
        }*/
        System.out.println("End of thread.");
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
     * Returns true if robots.txt permits page to be fetched
     * @param url full url of page to be fetched
     * @return true if permitted
     * @throws IOException
     */
    private boolean checkRobots(URL url) throws IOException {
        String path = null;
        URL robotsURL;
        try {
            robotsURL = new URL(url.getProtocol() + "://" + url.getHost() + "/robots.txt");
        } catch (MalformedURLException e) {
            return true;
        }
        //copy robots.txt
        InputStream in = robotsURL.openStream();
        boolean fetched = false;
        while(!fetched) {
            synchronized (lock) {
                try {
                    path = "./robots/" + robotsNumber + ".txt";
                    Files.copy(in, Paths.get(path));
                    fetched = true;
                }
                catch (FileAlreadyExistsException e){}
                finally {
                    robotsNumber++;
                }
            }
        }
        //read robots.txt
        String urlPathStr = url.getPath();
        String line = null;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
            do {
                line = bufferedReader.readLine();
            } while (line != null && !line.equals("User-agent: *"));
            if (line == null) {
                bufferedReader.close();
                return true;
            }
            //Read User-agent: *
            String subpath;
            do {
                line = bufferedReader.readLine();
                if (line != null && line.startsWith("Disallow: ")) {
                    subpath = line.substring(line.indexOf(":") + 2);
                    if(urlPathStr.startsWith(subpath)) { //URL path is disallowed by robots.txt
                        bufferedReader.close();
                        return false;
                    }
                }
            } while (line != null && !line.startsWith("User-agent: "));
            bufferedReader.close();
            return true;
        } catch (FileNotFoundException e) {
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Recupera la página en un URL
     * @param url URL de la página
     */
    private String fetchPage(URL url) throws IOException {
        String path = null;
        InputStream in = url.openStream();
        boolean fetched = false;
        while(!fetched) {
            synchronized (lock) {
                try {
                    path = "./pages/" + pageNumber + ".html";
                    Files.copy(in, Paths.get(path));
                    fetched = true;
                }
                catch (FileAlreadyExistsException e){}
                finally {
                    pageNumber++;
                }
            }
        }
        return path;
    }

    /**
     * Extrae el texto y el conjunto de enlaces de una página
     * @return Lista de links en la página, si es html
     */
    private Set<String> parsePage(URL url, String path) throws IOException {
        ///TODO agregar sólo .html
        FileReader reader = new FileReader(path);
        HashSet<String> linkSet = new HashSet<>();
        ParserDelegator parserDelegator = new ParserDelegator();
        HTMLEditorKit.ParserCallback parserCallback = new HTMLEditorKit.ParserCallback() {
            public void handleStartTag(HTML.Tag tag, MutableAttributeSet attribute, int pos) {
                if (tag == HTML.Tag.A) {
                    String address = (String) attribute.getAttribute(HTML.Attribute.HREF);
                    if(address != null && address.startsWith("/"))
                        address = url.getProtocol() + "://" + url.getHost() + address;
                    if(address != null && !address.startsWith("#"))
                        linkSet.add(address);
                }
            }
        };
        parserDelegator.parse(reader, parserCallback, false);
        return linkSet;
    }

    /**
     * Determina si un link extraído ya está en el URL frontier
     * @param link Link extraído
     * @return true si el link está en el URL frontier
     */
    private URL linkToURL(String link) throws MalformedURLException {
        System.out.println("Converting link to URL object.");
        if (urlFrontierAdmin.isInErrorList(link)) {
            return null;
        } else {
            return new URL(link);
        }
    }
}
