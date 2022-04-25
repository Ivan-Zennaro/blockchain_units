
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.Scanner;

import sun.misc.BASE64Encoder;

public class Main {
	
	public static final int LUNGHEZZACHIAVE = 20;
	
	public static final int MINPUBKEY = 36;
	public static final int MAXPUBKEY = 51;
	public static final int MINPRIVKEY = 46;
	public static final int MAXPRIVKEY = 66;
	
	public static final int PORTNODE = 1235;
	
	public static String myip ="";
	
	public static ArrayList<Block> blockchain = new ArrayList<Block>();
	public static HashMap<String,TransactionOutput> UTXOs = new HashMap<String,TransactionOutput>();	
	public static HashMap<String,TransactionOutput> UTXOlocal = new HashMap<String,TransactionOutput>();
	public static ArrayList<MinedPack> minedPacks = new ArrayList<MinedPack>();
	
	
	public static int difficulty = 5;
	public static double partialMiningTime = 0;			//variabile usata per tenere conto del tempo impiegato per minare N blocchi
	public static float minimumTransaction = 0.1f;
	private static boolean isThisTheFirstUser = true;	//serve per la transazione genesi
	public static int numberOfBLocksBetweenRewardAdjustement= 10000;

	
	public static boolean startMineProcess = false;
	public static float timeStampFirstPack = 0;
	public static int counterCoinbaseTrans = 0;
	
	public static Block currentBlock;
	public static int safeBlock = -1;
	
	public static Wallet coinbase ;
	
	private static Scanner keySelectMenu = new Scanner(System.in); 
	private static Scanner keySelectWallet = new Scanner(System.in); 
	private static Scanner keySelectValue = new Scanner(System.in); 
	
	private static PrintWriter output;
	private static ObjectOutputStream outputStreamObj;
	private static final String keysPath=".//keyPair.dat";
	private static final String blockchainPath=".//blockchain.dat";
	
	public static PrivateKey privateKey;
	public static PublicKey publicKey;
	public static byte[] publicKeyBytes;
	
	public static ArrayList<PublicKey> wallets = new ArrayList<PublicKey>();
	
	//lista nodi della rete. La conosce in partenza
	
    //public static String nodes [] = {"172.30.15.154", "172.30.15.153" , "172.30.15.155" , "172.30.15.152" , "172.30.15.151"}; //DA USARE IN LAB
	//public static String nodes [] = {"192.168.1.10", "192.168.1.6"};	
	public static String nodes [] = {"192.168.1.1", "192.168.1.2", "192.168.1.3", "192.168.1.4", "192.168.1.5"};	
	
	public static void main(String[] args) throws InterruptedException {
		
		if(args.length != 0)		//controllo, se c'è un paramtreo in args allora uso quello come ip
			myip=args[0];
		else{						//altrimenti prendo l'ip in automatico
			
			  try {
				myip= InetAddress.getLocalHost().getHostAddress();
				System.out.println("Running on local host: " + myip);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			
		}
		
		//printArray(nodes);
		removeMyIP();  //tolgo il mio ip dall array nodes. Lo faccio per comodità, per test in lab
		//printArray(nodes);
		
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider()); 
		
		if( !readKeyPair() ) { 								//se non esiste il file con la coppia di chiavi
			generateKeyPair();								//le genero			
			writeKeysToFile(publicKey, privateKey);		//le salvo sul file
		}else{												//se esiste mi faccio dare la blockchain da un nodo 
			try { 
				
				ObjectOutputStream outputStreamObj;
				ObjectInputStream inStreamObj;
				PrintWriter output;
				Scanner input;
				
				for(int i=0; i<nodes.length; i++) {	//contatto a ruota tutti i nodi
					
					Socket connection = new Socket();
					connection.connect( new InetSocketAddress( nodes[i]  , PORTNODE), 2000 ); //aspetta max 2s 
					
					outputStreamObj = new ObjectOutputStream(connection.getOutputStream());
					inStreamObj = new ObjectInputStream(connection.getInputStream());
					
					output = new PrintWriter(connection.getOutputStream(),true); 
					input = new Scanner(connection.getInputStream());
					
					//System.out.println("invio update");		//test
					
					output.println("!-UPDATEBLOCKCHAIN");
					Thread.sleep(200);			
					
					//System.out.println("inviato update");		//test
					
					MinedPack minedpack = (MinedPack) inStreamObj.readObject();
					Block b = (Block) inStreamObj.readObject();
					ArrayList<PublicKey> w = (ArrayList<PublicKey>)  inStreamObj.readObject();
					//System.out.println("ricevuti oggetti");   //test
					
					if( isChainValid(minedpack.blockchain) ){
						Main.blockchain = minedpack.blockchain;
						Main.UTXOs = minedpack.UTXOs;
						currentBlock=b; //se la catena è giusta mi salvo il blocco corrente che mi hanno inviato
						wallets = w;
						
						saveBlockchain();         //SALVO BLOCKCHAIN!
						
						input.close();
						output.close();
						outputStreamObj.close();
						inStreamObj.close();
						
						connection.close();
						
						break; 
					}
					
					input.close();
					output.close();
					outputStreamObj.close();
					inStreamObj.close();
					
					connection.close();
				} //end for
			
			
				
				
			} catch (ConnectException e) {
				//System.out.println("eccezione connessione");   //test
			} catch (Exception e) {
				
			}
			
			//in alternativa, se nessun altro nodo risponde con una catena (valida), ripristina quella che aveva salvato in locale
			readBlockchain();
		}
		
		coinbase = new Wallet();
		
		//aggiungo lui stesso in prima posizione
		if(wallets.size()==0) {		// se è la prima volta che avvio il nodo oppure se si è riavviato ma non c'è nessun altro online		
			wallets.add(publicKey);
		}
		
		if(currentBlock == null) {						// potrebbe non essere null se mi è arrivato da qualcun altro al riavvio 
			Block block = new Block("0", 0, difficulty); 
			currentBlock = block;
		}
		
		//processo server in ascolto di altri user
		NodeListenerUser nodeListenerUser = new NodeListenerUser();		
		nodeListenerUser.start();	
		
		//processo server in ascolto di altri nodi
		NodeListenerNode nodeListenerNode = new NodeListenerNode();		
		nodeListenerNode.start();
		
		//Invio a tutti la mia chiave pubblica
		
		sendToAllNode("!-DISCOVERKEY", null);
		
		System.out.println("This node's publicKey is " + StringUtil.getStringFromKey(publicKey).substring(MINPUBKEY, MAXPUBKEY));
		//System.out.println("This node's privateKey is " + StringUtil.getStringFromKey(privateKey).substring(MINPRIVKEY, MAXPRIVKEY)); //only for debug
		
		//il nodo può fare transazioni:	
		while(true) {
			
			
			System.out.println("-----------------------------");
			System.out.println("Press 1 to make a transaction");
			System.out.println("Press 2 to see your balance");
			System.out.println("Press 3 to see the chain");
			System.out.println("Press 4 to see UTXO");
			System.out.println("Press 5 to quit");
			System.out.println("-----------------------------");
			
			try {
				int inp=-1;
				try {	
					inp = keySelectMenu.nextInt();
				}catch (InputMismatchException e) {
					System.out.println("Input error, repeat the operation from beginning");
					keySelectMenu.next();
				}
			
				if(inp == 1) {
					System.out.println("Choose recipient:");
					for (int i = 0; i < wallets.size(); i++) { 
						System.out.println(i + ") " + StringUtil.getStringFromKey(wallets.get(i)).substring(MINPUBKEY,MAXPUBKEY));
					}
					
					int inp2;	//seleziona wallet
					do {
						 System.out.println("Insert a correct value:");
						 inp2 = keySelectWallet.nextInt(); 
					} while(inp2 < 0 || inp2 >= wallets.size());  //condizione per la quale continua a ciclare
							 
					System.out.println("Insert import:");
					float valueTrans = keySelectValue.nextFloat();
							 
				    System.out.println("Transaction info: ");
				    System.out.println("Send to " + StringUtil.getStringFromKey(wallets.get(inp2)).substring(MINPUBKEY,MAXPUBKEY) + " a value of: " + valueTrans);
				        	 
				     
				    Transaction transaction = generateTransaction(wallets.get(inp2) , valueTrans);	
				    
				    if(transaction != null) {
				    	Transaction copy = new Transaction (transaction.sender , transaction.reciepient , transaction.value , transaction.inputs);
						copy.signature = transaction.signature;
						copy.timeStamp = transaction.timeStamp;
									  
					    sendToAllNode("!-TRANSACTION", copy);					   
					    currentBlock.addTransaction(transaction);
				    }
				}//inp==1
				if(inp == 2) {
					System.out.println("Balance: " + getBalance());
				}
				if(inp == 3) {
					 showChain(currentBlock);
				}
			    if(inp == 4) { 
			    	System.out.println(printUtxos()); 
			    }
			    if(inp == 5) { //termino il programma, ma prima salvo blockchain e libero le porte 
			    	System.out.println("Closing...");
			    	saveBlockchain();
			    	try {
						nodeListenerUser.server.close();
						nodeListenerNode.server.close();
					} catch (IOException e) {					
						e.printStackTrace();
					}
			    	System.exit(0);		    	
			    } 	
				    	
			}catch (InputMismatchException e) {
				
				System.out.println("Input error, repeat the operation from beginning");
				keySelectMenu.reset();
			} 	    
			
		}//end while				
	}//end main
	
	
	//ancora da vedee se serve o no
	public static void addBlock(Block newBlock) {
		newBlock.mineBlock(difficulty);
		blockchain.add(newBlock);
	}

	
	public static void generateKeyPair() {
		try {
			//EDSCA Elliptic Curve Digital Signature Algorithm
			//BC Bouncy Castle apis for cryptography
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA","BC");
			//SHA-1 Pseudo-Random Number Generation
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			//tipologia di curva ellittica utilizzata
			ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime192v1");
			// Initializes the key pair generator for a certain keysize with the given source of randomness
			keyGen.initialize(ecSpec, random); //256 bit
			//genera coppia di chiavi
	        KeyPair keyPair = keyGen.generateKeyPair();
	        privateKey = keyPair.getPrivate();
	        publicKey = keyPair.getPublic();
		}catch(Exception e) {throw new RuntimeException(e);}
	}
	
	public static float getBalance() {
		float total = 0;	
        for (Map.Entry<String, TransactionOutput> item: UTXOs.entrySet()){  
        	
        	TransactionOutput UTXO = item.getValue();          		
            if(UTXO.isMine(publicKey) && UTXO.isTransactionConfirmed() ) { 
            	UTXOlocal.put(UTXO.id,UTXO);  
            	total = total + UTXO.value ; 	
            }
        }  
		return total;
	}
	
	public static Transaction generateTransaction(PublicKey _recipient,float value ) {
		//lavoro sull'utxos locale creato dal get balance
		if(getBalance() < value) {
			System.out.println("Not enought value");
			return null;
		}
		ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>();
		
		float total = 0;
		for (Map.Entry<String, TransactionOutput> item: UTXOlocal.entrySet()){
			TransactionOutput UTXO = item.getValue(); 
			total = total + UTXO.value;				  
			inputs.add(new TransactionInput(UTXO.id));
			if(total > value) break;				 
		}
		
		Transaction newTransaction = new Transaction(publicKey, _recipient , value, inputs);
		newTransaction.generateSignature(privateKey);

		for(TransactionInput input: inputs){
			UTXOlocal.remove(input.transactionOutputId);
		}
		
		return newTransaction;
	}
	
	
	public static void sendToAllNode (String command , Object o) {
		for(int i = 0 ; i < nodes.length ; i++) {
			if(nodes[i] != null) {
				NodeConnectionSender ncs = new NodeConnectionSender (command , i , o);
				ncs.start();
			}
		}
	}
	
	public static boolean isPresentPublicKey(PublicKey pc) {
		for (PublicKey k : wallets) {
			if (k.equals(pc))return true;
		}
		return false;
	}

	
	public static void showChain(Block currentBlock) {
		
		 ArrayList<Block> blockchainCopy = new ArrayList<Block>(blockchain);
		 blockchainCopy.add(currentBlock);
		 
		//mostro blocchi gi? minati
		int i=0;
		for(; i < blockchainCopy.size(); i++) {
			System.out.println("Blocco numero:" + i + "{");
			Block block=blockchainCopy.get(i);
			System.out.println("\t hash: " + block.hash);
			System.out.println("\t hash precedente: " + block.previousHash);
			System.out.println("\t merkle root: "+ block.merkleRoot);
			System.out.println("\t difficulty: " + block.difficulty);
			System.out.println("\t time-stamp: "+ block.timeStamp);
			System.out.println("\t nonce: "+ block.nonce);
			ArrayList<Transaction> transactions = blockchainCopy.get(i).transactions;
			System.out.println("\t transazioni{");
			for(int y=0; y < transactions.size(); y++) {
					
					System.out.println("\t \t id:" + transactions.get(y).transactionId);
					System.out.print("\t \t mittente:");

					System.out.println(StringUtil.getStringFromKey(transactions.get(y).sender).substring(MINPUBKEY, MAXPUBKEY));
					
					System.out.print("\t \t ricevente:");
					
					publicKeyBytes = transactions.get(y).reciepient.getEncoded();
					System.out.println(StringUtil.getStringFromKey(transactions.get(y).reciepient).substring(MINPUBKEY, MAXPUBKEY));
					
					
					System.out.println("\t \t valore:" + transactions.get(y).value);
					System.out.println("\t \t firma:" + transactions.get(y).signature.toString());
					System.out.println("\t \t transazioni Input {");
					try {
						for(int inputIndex = 0; inputIndex < transactions.get(y).inputs.size(); inputIndex++) {
							System.out.println("\t \t \t idOutput: ");
							System.out.println("\t \t \t " + transactions.get(y).inputs.get(inputIndex).transactionOutputId );
							System.out.print('\n');
						}
					}catch(Exception e) {System.out.println("\t \t \t inputs=null");}
					System.out.println("\t \t  }");
					
					System.out.println("\t \t transazioni Output {");
					
					try {
						for(int outputIndex = 0; outputIndex < transactions.get(y).outputs.size(); outputIndex++) {
							System.out.println("\t \t \t id: " + transactions.get(y).outputs.get(outputIndex).id );
							System.out.print("\t \t \t ricevente: ");
							
							
							System.out.println(StringUtil.getStringFromKey(transactions.get(y).outputs.get(outputIndex).reciepient).substring(MINPUBKEY, MAXPUBKEY) );
							
							System.out.println("\t \t \t valore: " + transactions.get(y).outputs.get(outputIndex).value );
							System.out.println("\t \t \t id genitore: " + transactions.get(y).outputs.get(outputIndex).parentTransactionId );
							System.out.print('\n');
						}
					}catch(Exception e) {System.out.println("\t \t \t outputs=null");}
					
					
					System.out.println("\t \t  }");
					System.out.print('\n');
			}//fine scorro transazioni	
			
			
			System.out.println("\t }");
			System.out.println("}");
			
		}//fine scorro blocchi
		
	}//fine show chain
	

	public static Boolean isChainValid(ArrayList<Block> blockchain) {
		
		//System.out.println("Verification of the chain in progress...");
		
		Block currentBlock; 
		Block previousBlock;
		String hashTarget;
		HashMap<String,TransactionOutput> tempUTXOs = new HashMap<String,TransactionOutput>(); 
		
		//scorro i blocchi
		for(int i = 0; i < blockchain.size(); i++) {
			
			currentBlock = blockchain.get(i);
			hashTarget = new String(new char[currentBlock.difficulty]).replace('\0', '0');  //es con diff=3 '000' con diff=2 '00'
			
			if (i == 0) {
				if(!currentBlock.previousHash.equals("0")) {
					System.out.println("#Error# First block must have 0 as previous block");
					return false;
				}
			}
			else {
				previousBlock = blockchain.get(i-1);
				//controllo hash del precendete
				if(!previousBlock.hash.equals(currentBlock.previousHash) ) {
					System.out.println("#Error# Previous hash:" + currentBlock.hash + "not corresponding");
					return false;
				}
			}
			
			//controllo hash del singolo blocco
			if(!currentBlock.hash.equals(currentBlock.calculateHash()) ){
				System.out.println("#Error# Current hash block: "+ currentBlock.hash +" not correct");
				return false;
			}
				
			
			//controllo se l'hash inizia come dovrebbe con gli 0
			if(!currentBlock.hash.substring( 0, currentBlock.difficulty).equals(hashTarget)) {
				System.out.println("#Error# Block "+ currentBlock.hash +" is not mined");
				return false;
			}
			
			//scorro transazioni interne
			TransactionOutput tempOutput; 
			for(int t = 0; t <currentBlock.transactions.size(); t++) {
				Transaction currentTransaction = currentBlock.transactions.get(t);  //transazione di appoggio 
				
				//verifica firma
				if(!currentTransaction.verifySignature()) {
					System.out.println("#Error# Signature of transaction(" + t + ") not valid");
					return false; 
				}
							
				//se non è una transazione da coinbase allora ha input 
				if(!currentTransaction.transactionId.equals("0")) {
					
					
					//controllo che i valori di input siano uguali a quelli di output
					if(currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
						System.out.println("#Error# Inputs not equals to outputs (" + t + ")");
						return false; 
					}
					
					//scorro le transazion di input
					for(TransactionInput input: currentTransaction.inputs) {	
						
						//assegno la transazione di output che ha generato quell'input
						tempOutput = tempUTXOs.get(input.transactionOutputId);
						
						//controllo se effettivamente l'input attuale deriva da un output precedente
						if(tempOutput == null) {
							System.out.println("#Error# Missing the reference to the input transaction (" + t + ")");
							return false;
						}
						
						//verifico se l'input ha lo stesso valore dell'output a cui faceva riferimento
						if(input.UTXO.value != tempOutput.value) {
							System.out.println("#Error# Transaction(" + t + ") has value not correct ");
							return false;
						}
						
						//fine controllo la elimino 
						tempUTXOs.remove(input.transactionOutputId);
					}
					
					//la transazione da coinbase ha slo un output
					if( currentTransaction.outputs.get(1).reciepient != currentTransaction.sender) {
						System.out.println("#Error# Transaction(" + t + ") has output that doesn't come back to the sender");
						return false;
					}	
				}
				
				//questo lo fa per tutte le transazioni
				
				//carico le transazioni di output sull hashmap temporaneo
				// al ciclare delle transazioni per continuare la verifica della chain
				for(TransactionOutput output: currentTransaction.outputs) {
					tempUTXOs.put(output.id, output);
				}
				
				if( currentTransaction.outputs.get(0).reciepient != currentTransaction.reciepient) {
					System.out.println("#Error# Transaction(" + t + ") output of recivient not correct");
					return false;
				}	
			}	
		}
		System.out.println("Chain Verification Succesful");
		return true;
	}
	

	
	public static String printUtxos(){
		String s="\n \t UTXOs:  ";
		for (Map.Entry<String, TransactionOutput> item: UTXOs.entrySet()){  
		    TransactionOutput UTXO = item.getValue();  
		    s+="\n \t \t id: " + UTXO.id ;
			s+="\n \t \t ricevente: " + StringUtil.getStringFromKey(UTXO.reciepient).substring(Main.MINPUBKEY, Main.MAXPUBKEY);
				
			s+="\n \t \t valore: " + UTXO.value ;
			s+="\n \t \t id genitore: " + UTXO.parentTransactionId ;
			s+="\n \t \t -------------------------------------------------";
        }
		return s;
    }
	
	
	public static synchronized void addMinedPack (MinedPack mp) {
		
		if ((mp.blockchain.size()-1) <= safeBlock) {
			System.out.println("It's just arrived a chain too late");
			return;
		}
		
		if(minedPacks.isEmpty()) {	
			
			//System.out.println(" first chain arrived from " + StringUtil.getStringFromKey(mp.publicKey).substring(MINPUBKEY, MAXPUBKEY));
				
			timeStampFirstPack = new Date().getTime();
			minedPacks.add(mp);
			
	    	new Thread (new Runnable() {
	    		public void run() {
	    			try {
						Thread.sleep(2000);
						boolean findCorrectChain = false;
						while (!findCorrectChain) {
							if(minedPacks.isEmpty()) {
								System.out.println("#No chain is valid");
								return;
							}
							MinedPack fasterMP = minedPacks.get(0);
							
							for(MinedPack mp : minedPacks) {
								if (mp.timestamp < fasterMP.timestamp) {
									fasterMP = mp;
								}
							}
							if(isChainValid(fasterMP.blockchain)) {
								
								findCorrectChain = true;
								blockchain = new ArrayList<Block>(fasterMP.blockchain);
								UTXOs = new HashMap<String,TransactionOutput>(fasterMP.UTXOs);
								
								//salvo il current block
								currentBlock = blockchain.get(blockchain.size()-1);
								safeBlock = blockchain.size()-1;  // numero blocco apposto
								
								//faccio transazione ricompensa	
								Wallet coinbase = new Wallet ();
								Transaction cbTrans = new Transaction(coinbase.publicKey, fasterMP.publicKey, rewardGenerator(safeBlock), null);
								cbTrans.generateSignature(coinbase.privateKey);	 

								cbTrans.transactionId = "0"; 
								cbTrans.outputs.add(new TransactionOutput(cbTrans.reciepient, cbTrans.value, cbTrans.transactionId, counterCoinbaseTrans++)); 
								
								//creo il blocco successivo tenendo conto dell'hash del blocco precedente 
								Block block = new Block (currentBlock.hash , currentBlock.number + 1, difficulty);
								currentBlock = block;
								
								Main.currentBlock.addTransaction(cbTrans);
								minedPacks.clear();
								
								saveBlockchain();         //SALVO BLOCKCHAIN!
								
							}else {minedPacks.remove(fasterMP);}
						}								
					} catch (InterruptedException e) {									
						System.out.println("Error:" +e.toString());
					};
	    		}
	    	}).start();   	
		}
		
		else {
			if (mp.timestamp < timeStampFirstPack + 2000) {
				minedPacks.add(mp);
				System.out.println("Another chain arrived from " + StringUtil.getStringFromKey(mp.publicKey).substring(MINPUBKEY, MAXPUBKEY));
			}	
		}
	}
	
	public static synchronized boolean firstUser(){		// restituisce true se si è connesso il primo utente
		boolean tem = isThisTheFirstUser;				// poi setta a false isThisTheFirstUser
		isThisTheFirstUser = false;						// perciò chi lo chiama dopo vede sempre false	
		return tem;										// viene chiamato anche se un nodo si vede arrivare !-NEWUSER
														// così non verranno fatte transazioni genesi per altri utenti, solo al primo!
	}
	
	
	public static void removeMyIP(){
		
		 String myIP = myip;
		
		/*
		  String myIP="";
		  try {
			myIP= InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		*/
		
		//System.out.println("||||| IP   " + myIP);	
		
		boolean found = false;
		
		int i; 
		for( i=0; i<nodes.length && !found; ++i){
			if( nodes[i].equals(myIP) ){
				found = true;
			}
		}
		
		i--;   // poiche prima di uscire dal for incrementa di 1 i
		
		if( found ){
			String nodesTemp []= new String[nodes.length - 1];
			
			for(int j=0; j<i; j++){
				nodesTemp[j] = nodes[j]; 
			}
			for(int j=i; j<nodesTemp.length; j++){
				nodesTemp[j] = nodes[j+1];
			}
			
			nodes = nodesTemp;
		}
		
		
	}

	public static void printArray(String [] s){
		String temp ="";
		for(int i=0; i<s.length; i++){
			temp+= s[i]+" - ";
		}
		System.out.println(temp);
	}
	
	public static void writeKeysToFile(Object pub, Object priv) {
		 
        try {
 
            FileOutputStream fileOut = new FileOutputStream(keysPath);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(pub);
            objectOut.writeObject(priv);
            objectOut.close();
 
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
	
	public static boolean readKeyPair() {
		File f = new File(keysPath);
		if(f.exists()) {
			FileInputStream fis;
			try {
				fis = new FileInputStream(keysPath);
				ObjectInputStream ois = new ObjectInputStream(fis);
				
				PublicKey pub  = (PublicKey) ois.readObject();
		        PrivateKey priv = (PrivateKey) ois.readObject();
		        System.out.println(StringUtil.getStringFromKey(pub).substring(MINPUBKEY, MAXPUBKEY));
		        //System.out.println(StringUtil.getStringFromKey(priv).substring(MINPRIVKEY, MAXPRIVKEY));
		        
		        publicKey=pub;
		        privateKey=priv;
		        
		        ois.close();
		        
			} catch (Exception e) {
				e.printStackTrace();
				return false;			
			}
			firstUser(); // lo chiamo per settare a false la variabile first user. Se trovo che questo nodo era già stato avviato  vuol dire che quello che si collega a me non è il primo utente in assoluto e non devo fargli la transazione genesi.
	        return true;
		}else {
			return false;
		}		
	}
	
	public static void saveBlockchain() {					//writes the blockchain to a file   
		 
        try {
 
            FileOutputStream fileOut = new FileOutputStream(blockchainPath);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            
            objectOut.writeObject(blockchain);
            objectOut.writeObject(UTXOs);
            objectOut.writeObject(currentBlock);
            
            objectOut.close();
         } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
	
	public static boolean readBlockchain() {
		File f = new File(blockchainPath);
		if(f.exists()) {
			FileInputStream fis;
			try {
				fis = new FileInputStream(blockchainPath);
				ObjectInputStream ois = new ObjectInputStream(fis);
				
				ArrayList<Block> b = (ArrayList<Block>) ois.readObject();
				HashMap<String,TransactionOutput> u = (HashMap<String,TransactionOutput>) ois.readObject();
				Block cb = (Block) ois.readObject();
		        
		        blockchain = b;
		        UTXOs = u;
		        currentBlock = cb;
		        
		        ois.close();
		        
			} catch (Exception e) {
				e.printStackTrace();
				return false;			
			}
			return true;
		}else {
			return false;
		}		
	}

	public static float rewardGenerator(int numOfBlocks) {
		if (numOfBlocks<2)return 500f;
		int numAdjustement=(int) Math.floor(numOfBlocks/numberOfBLocksBetweenRewardAdjustement);
		int adj=(int)Math.pow(2, numAdjustement);
		if(500f/adj>=0.1f) return 500f/adj;
		else return 0;		
	}
	
}
