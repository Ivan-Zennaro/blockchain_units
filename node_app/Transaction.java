import java.io.Serial;
import java.io.Serializable;
import java.security.*;
import java.util.ArrayList;
import java.util.Date;

public class Transaction implements Serializable {

    @Serial
    private static final long serialVersionUID = -5399605122490343339L;

    public String transactionId;
    public PublicKey sender;
    public PublicKey reciepient;
    public float value;
    /*
    Transaction signature: This is used to allow a malicious person to redirect the transaction to their own wallet and use your money
    */
    public byte[] signature;
    public long timeStamp;

    public ArrayList<TransactionInput> inputs;
    public ArrayList<TransactionOutput> outputs;


    public Transaction(PublicKey from, PublicKey to, float value, ArrayList<TransactionInput> inputs) {
        this.sender = from;
        this.reciepient = to;
        this.value = value;
        this.inputs = inputs;
        this.outputs = new ArrayList<TransactionOutput>();
        this.timeStamp = new Date().getTime();
    }

    public boolean processTransaction() {
        if (!verifySignature()) {
            System.out.println("#Wrong Signature");
            return false;
        }

        for (TransactionInput i : inputs) {
            //assign to the Unspent transaction output of the input transaction the corresponding
            // output transaction that generated it
            i.UTXO = Main.UTXOs.get(i.transactionOutputId);

        }

        // Check validity
        if (value < Main.minimumTransaction) {
            System.out.println("Input too small: " + value);
            System.out.println("Please insert a minimum value of: " + Main.minimumTransaction);
            return false;
        }

        // generate outputs
        float leftOver = getInputsValue() - value;
        transactionId = calulateHash();

        outputs.add(new TransactionOutput(this.reciepient, value, transactionId)); //send value to recipient

        if (leftOver == value) {
            // to avoid two output transaction with same Id
            outputs.add(new TransactionOutput(this.sender, leftOver, transactionId, 1));
        } else {
            outputs.add(new TransactionOutput(this.sender, leftOver, transactionId));
        }

        // UTXOs in main contains all the output transactions that arrive to the various users
        // coins from these transactions can be spent by the individual owners
        for (TransactionOutput o : outputs) {
            Main.UTXOs.put(o.id, o);
        }

        // delete old output transactions that have become input for new transactions
        for (TransactionInput i : inputs) {
            if (i.UTXO == null) continue; //if Transaction can't be found skip it
            Main.UTXOs.remove(i.UTXO.id);
        }

        return true;
    }

    public float getInputsValue() {
        float total = 0;
        for (TransactionInput i : inputs) {
            if (i.UTXO == null) continue;
            total = total + i.UTXO.value;
        }
        return total;
    }

    public void generateSignature(PrivateKey privateKey) {
        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(reciepient) + Float.toString(value);
        signature = StringUtil.applyECDSASig(privateKey, data);
    }

    public boolean verifySignature() {
        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(reciepient) + Float.toString(value);
        return StringUtil.verifyECDSASig(sender, data, signature);
    }

    public float getOutputsValue() {
        float total = 0;
        for (TransactionOutput output : outputs) {
            total = total + output.value;
        }
        return total;
    }

    private String calulateHash() {

        return StringUtil.applySha256(
                StringUtil.getStringFromKey(sender) +
                        StringUtil.getStringFromKey(reciepient) +
                        Float.toString(value) + this.timeStamp
        );
    }
}