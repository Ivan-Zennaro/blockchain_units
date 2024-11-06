import java.io.Serializable;

public class TransactionInput implements Serializable {

    //The input transaction comes from an output transaction
    //because to have money available to spend we must have
    //received it from someone, the id field refers to the id of the
    //output transaction that gave me the money

    public String transactionOutputId;  // reference to the output transaction id
    public TransactionOutput UTXO;      // Unspent transaction output

    public TransactionInput(String transactionOutputId) {
        this.transactionOutputId = transactionOutputId;
    }
}