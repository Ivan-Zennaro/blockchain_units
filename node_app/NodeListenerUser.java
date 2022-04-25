import java.net.*;

public class NodeListenerUser extends Thread{
	
	private static final int PORT= 1234; 
	public ServerSocket server = null;
				
	public NodeListenerUser(){}
	
	public void run(){
		
		try{ 
			
			server = new ServerSocket(PORT, 100, InetAddress.getByName( Main.myip ));						 
			while(true){		
				Socket connection = server.accept();
				System.out.println("Connection arriving from: " + connection.getInetAddress());
				UserConnection user = new UserConnection(connection);	
				user.start();				
				
			}//while
		} catch(Exception e) {System.out.println("NodeListenerUser:" + e.toString());} 
		
	}
}
