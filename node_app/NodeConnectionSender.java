import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.util.Scanner;

public class NodeConnectionSender extends Thread {

    public final int PORTNODE = 1235;
    public String command;
    public int indexNode;
    public Object obj;

    public NodeConnectionSender(String c, int i, Object o) {
        this.command = c;
        this.indexNode = i;
        this.obj = o;
    }

    public void run() {

        try {

            Socket connection = new Socket(Main.nodes[indexNode], PORTNODE);

            ObjectOutputStream outputStreamObj = new ObjectOutputStream(connection.getOutputStream());
            ObjectInputStream inStreamObj = new ObjectInputStream(connection.getInputStream());

            PrintWriter output = new PrintWriter(connection.getOutputStream(), true);
            Scanner input = new Scanner(connection.getInputStream());

            if (command.equals("!-DISCOVERKEY")) {

                output.println("!-DISCOVERKEY");
                Thread.sleep(100);
                outputStreamObj.writeObject(Main.publicKey);
                outputStreamObj.flush();
                PublicKey pc = (PublicKey) inStreamObj.readObject();
                if (!Main.isPresentPublicKey(pc)) {
                    Main.wallets.add(pc);
                }
            } else if (command.equals("!-NEWUSER") && obj != null) {

                output.println("!-NEWUSER");
                Thread.sleep(100);
                outputStreamObj.writeObject(obj);
                outputStreamObj.flush();
            } else if (command.equals("!-TRANSACTION") && obj != null) {

                output.println("!-TRANSACTION");
                Thread.sleep(100);
                outputStreamObj.writeObject(obj);
                outputStreamObj.flush();
            } else if (command.equals("!-MINED") && obj != null) {

                output.println("!-MINED");
                Thread.sleep(100);
                outputStreamObj.writeObject(obj);
                outputStreamObj.flush();
            }

            input.close();
            output.close();
            outputStreamObj.close();
            inStreamObj.close();
            connection.close();

        } catch (Exception e) {
            System.err.println("NodeConnectionSender:" + e);
        }

    }

}
