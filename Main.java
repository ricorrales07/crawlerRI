import java.io.FileNotFoundException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Ricardo on 30/8/2018.
 * <>
 */
public class Main {

    public static void main(String[] args) {
        //Instancia thread pool, variables compartidas y pone a los hilos a correr.

        //newCachedThreadPool crea, destruye y reusa hilos conforme sea necesario.
        ExecutorService threadPool = Executors.newCachedThreadPool();

        int F = 0, B = 0;
        String initialUrlsFile = "";

        // TODO: Pedir F y B?
        // TODO: Pedir archivo de URLs iniciales.

        URLFrontierAdmin urlFrontierAdmin;
        try {
            urlFrontierAdmin = new URLFrontierAdmin(F, B, initialUrlsFile);
        }
        catch (FileNotFoundException e) {
            // TODO: imprimir que no se encontró el archivo.
            return;
        }

        while (true) {
            synchronized (urlFrontierAdmin) {
                WebPage page = urlFrontierAdmin.getNextPage();
                if (page != null)
                    threadPool.submit(new Crawler(page, urlFrontierAdmin));
            }
        }
    }
}
