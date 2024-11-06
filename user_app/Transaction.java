import java.io.Serializable;
import java.security.*;
import java.util.ArrayList;
import java.util.Date;

public class Transaction implements Serializable {

    private static final long serialVersionUID = -5399605122490343339L;

    public String transactionId;
    public PublicKey sender;
    public PublicKey reciepient;
    public float value;
    public byte[] signature;

    public long timeStamp;
    public ArrayList<TransactionInput> inputs;
    public ArrayList<TransactionOutput> outputs;

    public Transaction(PublicKey from, PublicKey to, float value, ArrayList<TransactionInput> inputs) {
        this.sender = from;
        this.reciepient = to;
        this.value = value;
        this.inputs = inputs;
        this.outputs = new ArrayList<>();
        this.timeStamp = new Date().getTime();
    }

    public float getInputsValue() {
        float total = 0;
        for (TransactionInput i : inputs) {
            if (i.UTXO == null) continue;
            total = total + i.UTXO.value;
        }
        return total;
    }


    // sign with ECDSASig sender pri key+ data
    public void generateSignature(PrivateKey privateKey) {
        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(reciepient) + Float.toString(value);
        signature = StringUtil.applyECDSASig(privateKey, data);
    }

    // check signature with sender pub key  + data + old signature
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
                        StringUtil.getStringFromKey(reciepient) + value + timeStamp);
    }
}