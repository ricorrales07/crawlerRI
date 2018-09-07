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

        // TODO: Pedir F y B?

        URLFrontierAdmin urlFrontierAdmin = new URLFrontierAdmin(F, B);

        while (true) {
            synchronized (urlFrontierAdmin) {
                String url = urlFrontierAdmin.getNextURL();
                if (url != "")
                    threadPool.submit(new Crawler(url, urlFrontierAdmin));
            }
        }
    }
}
