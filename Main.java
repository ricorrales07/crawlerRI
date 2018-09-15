import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Ricardo on 30/8/2018.
 * <>
 */
public class Main {

    URLFrontierAdmin urlFrontierAdmin;

    public static void main(String[] args) {
        int F = 0;
        int B = 0;
        String initialUrlsFile = "";

        if(args.length != 3) {
            System.out.println("Error: número equivocado de argumentos.\nParámetros: F B initialUrlsFile");
            return;
        } else {
            try {
                F = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("F debe ser un entero.");
                return;
            }

            try {
                B = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("B debe ser un entero.");
                return;
            }

            initialUrlsFile = args[2];
        }

        new Main().run(F, B, initialUrlsFile);
    }

    void run(int F, int B, String initialUrlsFile) {
        //Instancia thread pool, variables compartidas y pone a los hilos a correr.

        //newCachedThreadPool crea, destruye y reusa hilos conforme sea necesario.
        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        System.out.println("Working Directory = " +
                System.getProperty("user.dir"));

        try {
            urlFrontierAdmin = new URLFrontierAdmin(F, B, initialUrlsFile);
        }
        catch (FileNotFoundException e) {
            System.out.println("Error: archivo no encontrado.");
            return;
        }
        catch (IOException e) {
            System.out.println("Error al leer archivo.");
            System.out.println(e.toString());
            return;
        }
        catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return;
        }

        while (true) {
            WebPage page = urlFrontierAdmin.getNextPage();
            if (page != null)
                threadPool.submit(new Crawler(page, urlFrontierAdmin));
        }
    }
}
