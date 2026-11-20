package model;
import java.io.Serializable;
import java.time.LocalDateTime;
public class Bid implements Serializable,Comparable<Bid>{
    private String bidId;
    private String auctionId;
    private Bidder bidder;
    private double amount;
    private LocalDateTime timestamp;
    public Bid(String bidId,String auctionId,Bidder bidder,double amount,LocalDateTime timestamp){
        this.bidId=bidId;
        this.auctionId=auctionId;
        this.bidder=bidder;
        this.amount=amount;
        this.timestamp=timestamp;
    }
    public String getBidId(){
        return bidId;
    }
    public String getAuctionId(){
        return auctionId;
    }
    public Bidder getBidder(){
        return bidder;
    }
    public double getAmount(){
        return amount;
    }
    public LocalDateTime getTimestamp(){
        return timestamp;
    }
    @Override
    public int compareTo(Bid other){
        return Double.compare(other.amount,this.amount);
    }
}
