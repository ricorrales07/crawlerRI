import java.net.InetAddress;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Ricardo on 5/9/2018.
 */
public class URLFrontierAdmin {

    private ArrayDeque<WebPage>[] frontQueues, backQueues;
    private int F, B;
    private ConcurrentHashMap<InetAddress, ArrayDeque<WebPage>> hostTable;
    //private PriorityQueue<InetAddress> priorityHeap;

    public URLFrontierAdmin(int F, int B) {
        this.F = F;
        this.B = B;
        frontQueues = new ArrayDeque[F];
        for (int i = 0; i < F; i++)
            frontQueues[i] = new ArrayDeque<WebPage>();
        backQueues = new ArrayDeque[B];
        for (int i = 0; i < B; i++)
            backQueues[i] = new ArrayDeque<WebPage>();
        hostTable = new ConcurrentHashMap<InetAddress, ArrayDeque<WebPage>>();
    }

    public String getNextURL() {
        return "";
    }

    private int getPriority(WebPage p) {
        double ranking = p.getRanking();
        int priority = (int) (ranking * F);

        return priority;
    }

    public void addPage(WebPage p) {
        frontQueues[getPriority(p)].add(p);
    }
}
