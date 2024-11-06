import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;

public class MinedPack implements Serializable {

    public ArrayList<Block> blockchain;
    public HashMap<String, TransactionOutput> UTXOs;
    public float timestamp;
    public PublicKey publicKey;  // for transaction reward

    public MinedPack(ArrayList<Block> bc, HashMap<String, TransactionOutput> utxos, float ts, PublicKey pc) {
        this.blockchain = new ArrayList<Block>(bc);
        this.UTXOs = new HashMap<String, TransactionOutput>(utxos);
        this.timestamp = ts;
        this.publicKey = pc;
    }

}
