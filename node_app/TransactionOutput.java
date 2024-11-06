import java.io.Serial;
import java.io.Serializable;
import java.security.PublicKey;

public class TransactionOutput implements Serializable {

    @Serial
    private static final long serialVersionUID = -5399605122490343339L;

    public String id;
    public PublicKey reciepient;        // new owner
    public float value;
    public String parentTransactionId;  // output txn id

    public int test = 0;

    //Constructor
    public TransactionOutput(PublicKey reciepient, float value, String parentTransactionId) {
        this.reciepient = reciepient;
        this.value = value;
        this.parentTransactionId = parentTransactionId;
        this.id = StringUtil.applySha256(StringUtil.getStringFromKey(reciepient) + Float.toString(value) + parentTransactionId + test);
    }

    public TransactionOutput(PublicKey reciepient, float value, String parentTransactionId, int test) {
        this.reciepient = reciepient;
        this.value = value;
        this.parentTransactionId = parentTransactionId;
        this.test = test;
        this.id = StringUtil.applySha256(StringUtil.getStringFromKey(reciepient) + Float.toString(value) + parentTransactionId + test);
    }

    // Check if I am the recipient
    public boolean isMine(PublicKey publicKey) {
        return (publicKey.equals(reciepient));
    }

    public boolean equals(Object x) {
        if (x instanceof TransactionOutput) {
            TransactionOutput to = (TransactionOutput) x;
            return to.reciepient.equals(this.reciepient) && to.id.equals(this.id) && (to.value == this.value) && to.parentTransactionId.equals(this.parentTransactionId) && (to.test == this.test);

        } else return false;
    }

    public boolean isTransactionConfirmed() {
        for (Transaction transaction : Main.currentBlock.transactions) {
            for (TransactionOutput transactionOutput : transaction.outputs) {
                if (transactionOutput.equals(this) && !transaction.sender.equals(transactionOutput.reciepient)) {  //se le due di output sono uguali e se il dest � uguale al mitt
                    return false;
                }
            }
        }
        return true;

    }
}