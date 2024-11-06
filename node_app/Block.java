import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class Block implements Serializable {

    private final int maxTransactionForBlock = 3;

    public String hash;
    public String previousHash;
    public String merkleRoot; //transaction tree
    public ArrayList<Transaction> transactions = new ArrayList<>();
    public long timeStamp;
    public int nonce;
    public int number;
    public int difficulty;
    public int transactionCount;

    //Block Constructor.
    public Block(String previousHash, int number, int difficulty) {
        this.previousHash = previousHash;
        this.timeStamp = new Date().getTime();
        this.hash = calculateHash();
        this.transactionCount = 0;
        this.number = number;
        this.difficulty = difficulty;
    }

    public String calculateHash() {
        return StringUtil.applySha256(previousHash + timeStamp + nonce + merkleRoot);
    }

    // increase nonce until mining completed
    public void mineBlock(int difficulty) {
        this.difficulty = difficulty;
        merkleRoot = StringUtil.getMerkleRoot(transactions);
        String target = StringUtil.getDificultyString(difficulty);
        long startMineTime = System.currentTimeMillis();
        System.out.println("|| Mining process started... ");
        while (!hash.substring(0, difficulty).equals(target)) {
            nonce++;
            hash = calculateHash();
            if (this.number <= Main.safeBlock) {
                System.out.println("|| The current block has already been mined");
                return;
            }
        }
        long endMineTime = System.currentTimeMillis();
        double mineTime = (endMineTime - startMineTime) / 1000.00;
        System.out.println("|| Mining complete in: " + mineTime + " seconds");
        adjustDifficulty(mineTime);
        if (this.number <= Main.safeBlock) {
            System.out.println("||  Mined but it's too late");
            return;
        }

        //if mined added to main List
        float timeOfMine = new Date().getTime();
		/* For testing
        System.out.println("Hash founded:" + hash );
        System.out.println("With nonce = " + nonce );
        */
        Main.blockchain.add(this);
        MinedPack minedpack = new MinedPack(Main.blockchain, Main.UTXOs, timeOfMine, Main.publicKey);
        Main.addMinedPack(minedpack);
        Main.sendToAllNode("!-MINED", minedpack);  // broadcast mined info
    }

    private void adjustDifficulty(double mineTime) {
        Main.partialMiningTime += mineTime;
        int everyNblock = 2; //adjust difficulty every 2 blocks
        if (this.number != 0 && (this.number % everyNblock) == 0) {
            double expectedTime = 7 * everyNblock; //seconds
            double gainTime = 6 * everyNblock; //seconds
            double waste = expectedTime - mineTime;
            if (waste > gainTime) {
                Main.difficulty++;
            } else if (waste < -gainTime) {
                Main.difficulty--;
            }
            Main.partialMiningTime = 0;
        }
    }

    public synchronized boolean addTransaction(Transaction transaction) {

        if (transaction == null || transactionCount >= maxTransactionForBlock)
            return false;

        // normal transaction have null Id. Coinbase transactions has id = 00
        if (transaction.transactionId == null) {
            if ((!transaction.processTransaction())) {
                System.out.println("Transaction failed");
                return false;
            }
        }

        //if is genesis then manually add output to Main UTXOs
        if (transaction.transactionId.equals("0")) {
            Main.UTXOs.put(transaction.outputs.getFirst().id, transaction.outputs.getFirst());
        }
        transactions.add(transaction);
        System.out.println("Transaction: " + transaction.value + " from " + StringUtil.getStringFromKey(transaction.sender).substring(Main.MINPUBKEY, Main.MAXPUBKEY) + " to " + StringUtil.getStringFromKey(transaction.reciepient).substring(Main.MINPUBKEY, Main.MAXPUBKEY));
        transactionCount++;
        if ((transactionCount == maxTransactionForBlock) || (this.number == 0)) { // first block need instant crystalization
            mineBlock(Main.difficulty);
        }
        Main.saveBlockchain();
        return true;
    }

}
