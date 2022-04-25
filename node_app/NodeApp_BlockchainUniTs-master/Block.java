import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class Block implements Serializable{
	
	private final int maxTransactionForBlock = 3;
	
	public String hash;															//hash del blocco
	public String previousHash; 												//hash precedente
	public String merkleRoot;													//albero delle transazioni 
	public ArrayList<Transaction> transactions = new ArrayList<Transaction>();  //lista transazioni
	public long timeStamp; 														
	public int nonce;
	public int number;
	public int difficulty;
	
	public int transactionCount;
	
	//Block Constructor.  
	public Block(String previousHash,int number, int difficulty) {
		this.previousHash = previousHash;
		this.timeStamp = new Date().getTime();		//millisecondi passati dal 1/1/1970.
		this.hash = calculateHash(); 				//inizialmente l'hash è calcolato in base a poche info
		transactionCount=0;
		this.number=number;
		this.difficulty = difficulty;
	}
	
	//Calcola hash in base alle info presenti
	public String calculateHash() {
		String calculatedhash = StringUtil.applySha256( 
				previousHash +
				Long.toString(timeStamp) +
				Integer.toString(nonce) + 
				merkleRoot
				);
		return calculatedhash;
	}
	
	//incremento il nonce fino a trovare l'hash desiderato
	public void mineBlock(int difficulty) {
		this.difficulty = difficulty;
		merkleRoot = StringUtil.getMerkleRoot(transactions);	    //trascrivo il merkleRoot delle transazioni
		String target = StringUtil.getDificultyString(difficulty);  //Create a string with difficulty * "0"
		long startMineTime = System.currentTimeMillis();
		System.out.println("|| Mining process started... ");
		while(!hash.substring( 0, difficulty).equals(target)) {		//verifico che l'hash inizi con il numero di 0 desiderati
			nonce ++;											    
			hash = calculateHash();	
			if(this.number <= Main.safeBlock) {
				System.out.println("|| The current block has already been mined");
				return;
			}
			
		}
		long endMineTime = System.currentTimeMillis();
		double mineTime = (endMineTime - startMineTime) /1000.00;
		System.out.println("|| Mining complete in: "+ mineTime + " seconds");
		
		adjustDifficulty(mineTime);
		
		if(this.number <= Main.safeBlock) {
			System.out.println("||  Mined but it's too late");
			return;
		}
		
		//se mino io lo aggiungo al mio vettore nel main
		float timeOfMine = new Date().getTime();
		//System.out.println("Hash founded:" + hash );
		//System.out.println("With nonce = " + nonce );
		Main.blockchain.add(this);
		MinedPack minedpack = new MinedPack(Main.blockchain, Main.UTXOs, timeOfMine, Main.publicKey);
		Main.addMinedPack(minedpack);
		Main.sendToAllNode("!-MINED", minedpack);  //invio a tutti le info di minaggio
	}
	
	private void adjustDifficulty(double mineTime) {
		Main.partialMiningTime += mineTime; 
		
		int everyNblock = 2; 		//ogni quanti blocchi aggiusta la difficoltà
		
		if( this.number!=0 && (this.number % everyNblock) == 0) {
			double expectedTime = 7 * everyNblock ; 			//second
			double gainTime = 6 * everyNblock;					//second.  Un po' di lasco/gioco 
			double waste = expectedTime - mineTime;
			
			if( waste > gainTime ) {
				Main.difficulty++;
			}else if( waste < -gainTime   ){
				Main.difficulty--;
			}
			Main.partialMiningTime=0;
		}
	}

	public boolean timeToChangeBlock() {
		if(transactionCount >= maxTransactionForBlock) 
			return true; 
		else return false;
	}
	
	
	//sincrhonize dda vedere se va bene o no
	public synchronized boolean addTransaction(Transaction transaction) {
		
		
		if(transaction == null || timeToChangeBlock()) return false;
			
		//le transazioni classiche partono con id nullo quelle da coinbase no hanno id=0
		if(transaction.transactionId == null) { 	 			//se transazioni normale (non da coinbase)
			if((transaction.processTransaction() != true)) {	//se il processo di transazione non va a buon fine	
				System.out.println("Transaction failed");
				return false;
			}
		}
		
		//se è genesi allora aggiungo manualmente gli output su Main UTXOs
		if (transaction.transactionId.equals("0")) {
			Main.UTXOs.put(transaction.outputs.get(0).id, transaction.outputs.get(0));
		}
			
		transactions.add(transaction);
		
		System.out.println("Transaction: "+ transaction.value+ " from " + StringUtil.getStringFromKey(transaction.sender).substring(Main.MINPUBKEY, Main.MAXPUBKEY) + " to " + StringUtil.getStringFromKey(transaction.reciepient).substring(Main.MINPUBKEY, Main.MAXPUBKEY) );	
		transactionCount++;
		
		if( (transactionCount == maxTransactionForBlock) || (this.number == 0) ){ // se questo è il blocco zero cristallizzo subito
			mineBlock(Main.difficulty);
		}
		
		Main.saveBlockchain();         //SALVO BLOCKCHAIN!
		
		return true;
	}
	
}
