import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner; 
import java.io.*; 
import java.net.*;
import java.security.*;

public class UserConnection extends Thread{
	
	
	public ObjectOutputStream outputStreamObj;		// oggetti in uscita
	public ObjectInputStream inStreamObj;			// oggetti in arrivo
	public Scanner input;	   						// testo in arrivo
	public PrintWriter output; 						// testo in uscita
		
	public Socket connection;	
	public PublicKey publicKey;
	public HashMap<String,TransactionOutput> UTXOs = new HashMap<String,TransactionOutput>();


	public UserConnection(Socket c) throws InterruptedException{
		this.connection = c;
	}
	
	public void run(){
		
		try{
			
			outputStreamObj = new ObjectOutputStream(connection.getOutputStream());
			inStreamObj = new ObjectInputStream(connection.getInputStream());
			
			output = new PrintWriter(this.connection.getOutputStream(),true); 
			input = new Scanner(this.connection.getInputStream());				
			
			while(true){	
					
				//System.out.println("Waiting Inputs from User:");
				do {} while (! input.hasNextLine());
				
				String dataFromUser = input.nextLine();  
				//System.out.println(dataFromUser);
				
				//richiesta connessione
				
				if (dataFromUser.equals("?-CONNECTION")){
					
					output.println("OK-CONNECTION");	//accetto la connessione con l'utente
					
					//setto la chiave publica dell'utente
					this.publicKey = (PublicKey) inStreamObj.readObject();
					if(!Main.isPresentPublicKey(this.publicKey)) {Main.wallets.add(this.publicKey);}
					
					Main.sendToAllNode("!-NEWUSER",this.publicKey); //avviso i nodi di un nuovo utente
			
					if( Main.firstUser() ) {  //allora transazione genesi al primo
						
													
						Transaction cbTrans = new Transaction(Main.coinbase.publicKey, publicKey, 100f, null);
						cbTrans.generateSignature(Main.coinbase.privateKey);	 //manually sign the genesis transaction	
								
						cbTrans.transactionId = "0"; 
						cbTrans.outputs.add(new TransactionOutput(cbTrans.reciepient, cbTrans.value, cbTrans.transactionId)); 
									
						Main.currentBlock.addTransaction(cbTrans);
						
						Main.sendToAllNode("!-TRANSACTION",cbTrans);
					}
				}
								
				//richiesta disconnessione
				else if (dataFromUser.equals("?-DISCONNECTION")){
					this.connection.close();
				}
						
				else if (dataFromUser.equals("?-WALLETS")){
				    output.println("OK-WALLETS");
					Thread.sleep(200);
					ArrayList<PublicKey> wallets = new ArrayList<PublicKey>(Main.wallets);
					outputStreamObj.writeObject(wallets);	
				}
								
				//richiesta transazione
				else if (dataFromUser.equals("?-TRANSACTION")){
					
					//ricevo un oggetto transazione senza input
					
					Transaction transaction = (Transaction) inStreamObj.readObject();
					
					//creo la transazione di copia da inviare e far processare dagli altri nodi
					
					Transaction copy = new Transaction (transaction.sender , transaction.reciepient , transaction.value , null);
					copy.signature = transaction.signature;
					copy.timeStamp = transaction.timeStamp;
					
					
					new Thread (new Runnable() {
			    		public void run() {
			    			try {
			    				//se input aggiunti correttamnte
								if (addInputsToTransaction(transaction)) {
									copy.inputs = new ArrayList<TransactionInput>(transaction.inputs);
									
									Main.sendToAllNode("!-TRANSACTION",copy);
									
									if (Main.currentBlock.addTransaction(transaction)) {
										
										output.println("TRANSACTION-CONFIRMED");
										output.flush();
										
									}else {
										output.println("TRANSACTION-REFUSED");
									}	
								}
								else {System.out.println("Impossible to add Inputs");}
			    			}catch (Exception e) {	
							System.out.println("Error:" +e.toString());
			    			}
			    		}
			    	}).start();  
				}
				
				//RICHIESTA SALDO
				else if (dataFromUser.equals("?-BALANCE")){
					output.println("OK-BALANCE");
					Thread.sleep(200);
					output.println(getBalance()+"");													
				}
				
				else if(dataFromUser.equals("?-INFOTRANS")) {
					output.println("OK-TRANSINFO");
					Thread.sleep(200);
					String s = "Confirmed Transactions:";					
					for(Block block : Main.blockchain ) {
						for(Transaction transaction : block.transactions) {
							if(transaction.sender.equals(this.publicKey)) {
								s = s + '\n' + "Sended to " + StringUtil.getStringFromKey(transaction.reciepient).substring(Main.MINPUBKEY, Main.MAXPUBKEY) + " a value of " + transaction.value;
							}
							if(transaction.reciepient.equals(this.publicKey)) {
								s = s + '\n' + "Recieved from " + StringUtil.getStringFromKey(transaction.sender).substring(Main.MINPUBKEY, Main.MAXPUBKEY) + " a value of " + transaction.value;
							}
						}
					}
					s = s + '\n' + "-----------------------------";
					s = s + '\n' + "Transactions not verify:";
					for(Transaction transaction : Main.currentBlock.transactions) {
						if(transaction.sender.equals(this.publicKey)) {
							s = s + '\n' + "Sended to " + StringUtil.getStringFromKey(transaction.reciepient).substring(Main.MINPUBKEY, Main.MAXPUBKEY) + " a value of " + transaction.value;
						}
						if(transaction.reciepient.equals(this.publicKey)) {
							s = s + '\n' + "Recieved from " + StringUtil.getStringFromKey(transaction.sender).substring(Main.MINPUBKEY, Main.MAXPUBKEY) + " a value of " + transaction.value;
						}
					}
					outputStreamObj.writeObject(s);
					outputStreamObj.flush();
				}
				
			}//while
		}//try
		catch(Exception e){System.out.println("UserConnection: "+e.toString());}
	}//run
	
	
	public float getBalance() {
		float total = 0;	
        for (Map.Entry<String, TransactionOutput> item: Main.UTXOs.entrySet()){
        	TransactionOutput UTXO = item.getValue();  
            if(UTXO.isMine(publicKey) && UTXO.isTransactionConfirmed() ) {      
            	UTXOs.put(UTXO.id,UTXO);      
            	total = total + UTXO.value ; 	
            }
        }  
		return total;
	}	
	
	public boolean addInputsToTransaction ( Transaction transaction ) {
		
		//con getBalance aggiorno l'UTXOs locale e lavoro su quello locale  
		
		if( getBalance() < transaction.value ) {
			System.out.println("Impossible no value left");
			return false;
		}
			
		float total = 0;
		for (Map.Entry<String, TransactionOutput> item: UTXOs.entrySet()){ //l'UTXOs è quello locale

			if (item.getValue()==null) {
				System.out.println("No expendable transaction avaiable");
			}
			TransactionOutput UTXO = item.getValue(); 
			total = total + UTXO.value;				  
			transaction.inputs.add(new TransactionInput(UTXO.id));
			if(total > transaction.value) break;
		}
				
			//rimuovo dalla mappa delle entrate quelle utilizzate
			for(TransactionInput input: transaction.inputs){
				UTXOs.remove(input.transactionOutputId);
			}
		return true;
	}
}
