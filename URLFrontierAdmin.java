import java.io.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Ricardo on 5/9/2018.
 */
public class URLFrontierAdmin {

    private class PriorityHeapItem {
        public Instant nextContact;
        public InetAddress ip;

        public PriorityHeapItem(Instant nextContact, InetAddress ip) {
            this.nextContact = nextContact;
            this.ip = ip;
        }

        public boolean equals(Object o) {
            if (!(o instanceof PriorityHeapItem))
                return false;
            else
                return ip.equals(((PriorityHeapItem)o).ip);
        }
    }

    private class PriorityHeapItemComparator implements Comparator<PriorityHeapItem> {
        public int compare(PriorityHeapItem a, PriorityHeapItem b) {
            return a.nextContact.compareTo(b.nextContact);
        }
    }

    private ArrayDeque<WebPage>[] frontQueues, backQueues;
    private int F, B;
    private ConcurrentHashMap<InetAddress, ArrayDeque<WebPage>> hostTable;
    private PriorityQueue<PriorityHeapItem> priorityHeap;
    private Random randomGenerator;
    private ArrayList<String> errorList;
    private ArrayList<WebPage> processingList;

    public URLFrontierAdmin(int F, int B, String initialUrlsFile)
            throws IOException, IllegalArgumentException {
        this.F = F;
        this.B = B;
        frontQueues = new ArrayDeque[F];
        for (int i = 0; i < F; i++)
            frontQueues[i] = new ArrayDeque<WebPage>();
        backQueues = new ArrayDeque[B];
        for (int i = 0; i < B; i++)
            backQueues[i] = new ArrayDeque<WebPage>();
        hostTable = new ConcurrentHashMap<InetAddress, ArrayDeque<WebPage>>();
        priorityHeap = new PriorityQueue<>(10000,
                new PriorityHeapItemComparator());
        randomGenerator = new Random(Instant.now().getEpochSecond());
        errorList = new ArrayList<String>();
        processingList = new ArrayList<WebPage>();

        loadQueues(initialUrlsFile);
    }

    private void loadQueues(String initialUrlsFile)
            throws IOException, IllegalArgumentException {
        BufferedReader reader = new BufferedReader(new FileReader(initialUrlsFile));

        String s;
        int queueNumber = 0;
        while ((s = reader.readLine()) != null && queueNumber < B) {
            WebPage page;
            try {
                page = new WebPage(s);
                InetAddress pageIp = InetAddress.getByName(page.getURL().getHost());
                if (hostTable.containsKey(pageIp)) {
                    ArrayDeque<WebPage> q = hostTable.get(pageIp);
                    q.add(page);
                }
                else {
                    ArrayDeque<WebPage> q = backQueues[queueNumber];
                    q.add(page);
                    hostTable.put(pageIp, q);
                    priorityHeap.add(new PriorityHeapItem(Instant.now(), pageIp));
                    queueNumber++;
                }
            }
            catch (MalformedURLException e) {
                errorList.add(s);
            }
        }
        if (s == null) {
            throw new IllegalArgumentException("No hay suficientes URLs para " +
                "llenar las back queues. Pruebe con más URLs o un valor de B " +
                "más pequeño.");
        }
        else {
            do {
                try {
                    frontQueues[0].add(new WebPage(s));
                }
                catch (MalformedURLException e) {
                    errorList.add(s);
                }
            } while ((s = reader.readLine()) != null);
        }
    }

    public WebPage getNextPage() {
        System.out.println("Fetching next page...");
        PriorityHeapItem item = priorityHeap.poll();
        System.out.println("Next IP address in priority heap: " + item.ip.toString()
            + "; Time of contact: " + item.nextContact.getEpochSecond());
        if (item.nextContact.compareTo(Instant.now()) > 0) {
            long milliseconds = Instant.now().until(item.nextContact, ChronoUnit.MILLIS);
            System.out.println(milliseconds + " left for contact. Sleeping thread.");
            try {
                Thread.sleep(milliseconds);
            }
            catch (InterruptedException e) {
                // No debería ocurrir.
            }
            System.out.println("Thread woke up. Continuing with next page fetch.");
        }

        ArrayDeque<WebPage> backQueue = hostTable.get(item.ip);
        //System.out.println("Back Queue selected: " + backQueue); //sería bueno tener el número
        WebPage nextPage = backQueue.remove();
        processingList.add(nextPage);
        System.out.println("Page dequed: " + nextPage.getURL().toString());

        if (backQueue.isEmpty()) {
            System.out.println("Back queue is empty. Refilling back queues.");
            hostTable.remove(item.ip);
            refillBackQueues(backQueue);
        }

        // TODO: Revisar si esta cantidad de minutos funciona.
        item.nextContact = Instant.now().plus(5, ChronoUnit.SECONDS);
        priorityHeap.add(item);
        System.out.println("Next time to contact IP " + item.ip.toString()
            + ": " + item.nextContact.getEpochSecond());

        return nextPage;
    }

    private void refillBackQueues(ArrayDeque<WebPage> backQueue) {
        InetAddress ip = InetAddress.getLoopbackAddress(); //Para que Java no se queje.

        while (backQueue.isEmpty()) {
            WebPage p = null;
            while (p == null) { // Si la araña es continua, debería obtener algún elemento algún día.
                int n = pickRandomFrontQueue();
                System.out.println("Random front queue picked: " + n);
                ArrayDeque<WebPage> frontQueue = frontQueues[n];
                p = frontQueue.poll();
                System.out.println((p == null) ? "Empty queue, choosing another one." :
                        "Page dequeued: " + p.getURL().toString());
            }

            try {
                ip = InetAddress.getByName(p.getURL().getHost());
                System.out.println("IP address corresponding to web page: " + ip.toString());
            }
            catch (UnknownHostException e){
                System.out.println("Could not determine IP address corresponding to web page."
                    + " Sending page to error list.");
                errorList.add(p.getURL().toString());
                continue;
            }

            if (hostTable.containsKey(ip)) {
                System.out.println("IP already found in back queues. Adding web page"
                    + " to back queue.");
                ArrayDeque<WebPage> b = hostTable.get(ip);
                b.add(p);
            }
            else {
                System.out.println("IP not found in back queues. Adding to current"
                    + "empty back queue.");
                hostTable.put(ip, backQueue);
                backQueue.add(p);
            }
        }

        PriorityHeapItem i = new PriorityHeapItem(Instant.now(), ip);
        if (!priorityHeap.contains(i))
            priorityHeap.add(i);
    }

    private int pickRandomFrontQueue() {
        int totalPossibilities = (F * (F + 1)) / 2;
        int f = randomGenerator.nextInt(totalPossibilities);

        // Si 0 <= f < F, se escoge la cola 0 (F posibilidades).
        // Si F <= f < 2F-1, se escoge la cola 1 (F-1 posibilidades).
        // Si 2F-1 <= f < 3F-3, se escoge la cola 2 (F-2 posibilidades).
        // ...
        // Si (F * (F + 1)) / 2 - 1 <= f < (F * (F + 1)) / 2, se escoge la cola F (1 posibilidad).
        for (int i = 0; i < F; i++){
            int start = i * F - ((i - 1) * i) / 2;
            int end = (i + 1) * F - (i * (i + 1)) / 2;
            if (f >= start && f < end) {
                return i;
            }
        }

        // No debería llegar aquí.
        return 0;
    }

    private int getPriority(WebPage p) {
        double ranking = p.getRanking();
        int priority = (int) ((1 - ranking) * F);

        return priority;
    }

    public void addPage(WebPage p) {
        if (processingList.contains(p))
            processingList.remove(p);
        frontQueues[getPriority(p)].add(p);
    }

    public boolean isInErrorList(String url) {
        return errorList.contains(url);
    }

    public void addToErrorList(String url) {
        errorList.add(url);
    }

    public WebPage find(URL url) {
        WebPage fake = new WebPage(url);

        if (processingList.contains(fake))
            return processingList.get(processingList.indexOf(fake));
        else {
            for (ArrayDeque<WebPage> q : frontQueues) {
                if (q.contains(fake))
                    for (WebPage p : q)
                        if (p.equals(fake))
                            return p;
            }
            for (ArrayDeque<WebPage> q : backQueues) {
                if (q.contains(fake))
                    for (WebPage p : q)
                        if (p.equals(fake))
                            return p;
            }
        }

        return null;
    }
}
