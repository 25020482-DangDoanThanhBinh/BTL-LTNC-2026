package network;
import java.io.Serializable;
public class Message implements Serializable{
    private static final long serialVersionUID=1L;
    public enum Type{
        JOIN,
        LOGIN,
        LOGIN_RESULT,
        REGISTER,
        REGISTER_RESULT,
        SYNC_STATE,
        CREATE_AUCTION,
        BID,
        START_AUCTION,
        FINISH_AUCTION,
        PAY_AUCTION,
        CANCEL_AUCTION,
        UPDATE,
        ERROR,
        INFO
    }
    private final Type type;
    private final String requestId;
    private final String auctionId;
    private final String sender;
    private final double amount;
    private final String content;
    private final Object payload;
    public Message(Type type,String auctionId,String sender,double amount,String content){
        this(type,null,auctionId,sender,amount,content,null);
    }
    public Message(Type type,String requestId,String auctionId,String sender,double amount,String content,Object payload){
        this.type=type;
        this.requestId=requestId;
        this.auctionId=auctionId;
        this.sender=sender;
        this.amount=amount;
        this.content=content;
        this.payload=payload;
    }
    public Type getType(){
        return type;
    }
    public String getRequestId(){
        return requestId;
    }
    public String getAuctionId(){
        return auctionId;
    }
    public String getSender(){
        return sender;
    }
    public double getAmount(){
        return amount;
    }
    public String getContent(){
        return content;
    }
    public Object getPayload(){
        return payload;
    }
}
