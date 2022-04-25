import java.util.ArrayList;
import java.util.Scanner; 
import java.io.*; 
import java.net.*;
import java.security.PublicKey; 



public class NodeConnection extends Thread{ 
	

	public ObjectOutputStream outputStreamObj;		//oggetti in uscita
	public ObjectInputStream inStreamObj;	
	public Scanner input;	   						// testo in arrivo
	public PrintWriter output; 						//testo in uscita

	public Socket connection;											
	
	public NodeConnection(Socket c) throws InterruptedException{		
		connection = c;	
	}
	
	public void run(){
		
		try{
		
			outputStreamObj = new ObjectOutputStream(connection.getOutputStream());
			inStreamObj = new ObjectInputStream(connection.getInputStream());
			
			output = new PrintWriter(this.connection.getOutputStream(),true); 
			input = new Scanner(this.connection.getInputStream());			

			
			//System.out.println("Waiting Inputs from Nodes:");
			do {} while (! input.hasNextLine());
				
			String dataFromOtherNode = input.nextLine();  				
			//System.out.println(dataFromOtherNode);
			
			//un nuovo nodo si connette a me e mi invia la sua pubblica 
			if(dataFromOtherNode.equals("!-DISCOVERKEY")) {
				PublicKey pc = (PublicKey) inStreamObj.readObject();
				if(!Main.isPresentPublicKey(pc)) {Main.wallets.add(pc);}				
				outputStreamObj.writeObject(Main.publicKey);
				outputStreamObj.flush();	
			}

					
			else if(dataFromOtherNode.equals("!-TRANSACTION")) {
				Transaction transaction = (Transaction) inStreamObj.readObject();
				Main.currentBlock.addTransaction(transaction);
			}
							
			//supponiamo che i nodi non si possono disconnettere --> Sempre Online
			
			else if(dataFromOtherNode.equals("!-NEWUSER")) {
				Main.firstUser(); // così non fa altre transazioni genesi se non al primissimo utente che si collega alla rete di nodi
				PublicKey pc = (PublicKey) inStreamObj.readObject();
				if(!Main.isPresentPublicKey(pc)) {Main.wallets.add(pc);}
			}
			
			else if(dataFromOtherNode.equals("!-MINED")) {				
				MinedPack minedpack = (MinedPack) inStreamObj.readObject();
				Main.addMinedPack(minedpack);	
			}
			
			else if(dataFromOtherNode.equals("!-UPDATEBLOCKCHAIN")) {				
				MinedPack minedpack = new MinedPack(Main.blockchain, Main.UTXOs, 0, null);
				
				outputStreamObj.writeObject(minedpack);		
				outputStreamObj.flush();
				
				outputStreamObj.writeObject(Main.currentBlock);		
				outputStreamObj.flush();
				
				outputStreamObj.writeObject(Main.wallets);		
				outputStreamObj.flush();
			}
			
			
			

		
		}//try
		catch(Exception e){System.out.println("NodeConnection: "+e.toString());}
	}//run


}
