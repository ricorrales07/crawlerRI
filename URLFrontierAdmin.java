import java.util.Queue;

/**
 * Created by Ricardo on 5/9/2018.
 */
public class URLFrontierAdmin {

    private Queue[] frontQueues, backQueues;
    public int F, B;

    public URLFrontierAdmin(int F, int B) {
        this.F = F;
        this.B = B;
        frontQueues = new Queue[F];
        backQueues = new Queue[B];
    }

    public String getNextURL() {
        return "";
    }
}
