import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;

public class NodeListenerNode extends Thread {

    private static final int PORTNODE = 1235;
    public ServerSocket server = null;

    public NodeListenerNode() {
    }

    public void run() {

        try {
            server = new ServerSocket(PORTNODE, 100, InetAddress.getByName(Main.myip));
            while (true) {
                Socket connection = server.accept();
                NodeConnection node = new NodeConnection(connection);
                node.start();
            }//while
        } catch (Exception e) {
            System.out.println("NodeListenerNode:" + e.toString());
        }
    }
}
