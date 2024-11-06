import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.security.PublicKey;

public class UserListener extends Thread {

    private ObjectInputStream inStreamObj = null;
    private Scanner input = null;

    public UserListener(Scanner inputStr, ObjectInputStream inStreamObj) {
        this.input = inputStr;
        this.inStreamObj = inStreamObj;
    }

    public void run() {

        while (true) {

            do {
            } while (!input.hasNextLine());

            String stringFromNode = input.nextLine();

            if (stringFromNode.equals("OK-CONNECTION")) {
                UserMain.isConnect = true;
                System.out.println("Connection accepted");
            }

            if (stringFromNode.equals("OK-WALLETS")) {

                ArrayList<PublicKey> wallets;

                try {
                    wallets = (ArrayList<PublicKey>) inStreamObj.readObject();
                    UserMain.walletsArrive = true;
                    UserMain.wallets = new ArrayList<PublicKey>(wallets);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            if (stringFromNode.equals("OK-BALANCE")) {

                String saldoInfo = input.nextLine();
                UserMain.BalanceArrive = true;
                UserMain.myBalance = Float.parseFloat(saldoInfo);
            }

            if (stringFromNode.equals("TRANSACTION-CONFIRMED")) {
                System.out.println("Transaction Accepted");
            }

            if (stringFromNode.equals("TRANSACTION-REFUSED")) {
                System.out.println("Transaction Refused");
            }

            if (stringFromNode.equals("OK-TRANSINFO")) {
                String s;
                try {
                    s = (String) inStreamObj.readObject();
                    System.out.println(s);
                } catch (Exception e) {
                    System.out.println(e.toString());
                }
            }
        }//while(true)
    }
}
