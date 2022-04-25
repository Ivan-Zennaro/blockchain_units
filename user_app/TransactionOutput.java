import java.io.Serializable;
import java.security.PublicKey;

public class TransactionOutput implements Serializable{
	
	
	public String id;
	public PublicKey reciepient; 		
	public float value; 		 		
	public String parentTransactionId;  
	
	public int test;
	
	//Constructor
	public TransactionOutput(PublicKey reciepient, float value, String parentTransactionId) {
		this.reciepient = reciepient;
		this.value = value;
		this.parentTransactionId = parentTransactionId;
		this.id = StringUtil.applySha256(StringUtil.getStringFromKey(reciepient)+Float.toString(value)+parentTransactionId + test);
	}
	
	public boolean isMine(PublicKey publicKey) {
		return (publicKey.equals(reciepient));
	}
	
}