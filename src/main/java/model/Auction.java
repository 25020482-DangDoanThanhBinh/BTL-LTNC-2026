package model;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
public class Auction implements Serializable{
    private static final long serialVersionUID=1L;
    private String auctionId;
    private Item item;
    private Seller seller;
    private Bidder highestBidder;
    private double currentPrice;
    private double bidIncrement;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private List<Bid> bids;
    public Auction(String auctionId,Item item,Seller seller,double bidIncrement,LocalDateTime startTime,LocalDateTime endTime){
        if(bidIncrement<=0){
            throw new IllegalArgumentException("Buoc gia phai lon hon 0");
        }
        this.auctionId=auctionId;
        this.item=item;
        this.seller=seller;
        this.currentPrice=item.getStartingPrice();
        this.bidIncrement=bidIncrement;
        this.startTime=startTime;
        this.endTime=endTime;
        this.status=AuctionStatus.OPEN;
        this.bids=new ArrayList<>();
    }
    public String getAuctionId(){
        return auctionId;
    }
    public Item getItem(){
        return item;
    }
    public Seller getSeller(){
        return seller;
    }
    public Bidder getHighestBidder(){
        return highestBidder;
    }
    public double getCurrentPrice(){
        return currentPrice;
    }
    public double getBidIncrement(){
        return bidIncrement;
    }
    public LocalDateTime getStartTime(){
        return startTime;
    }
    public LocalDateTime getEndTime(){
        return endTime;
    }
    public AuctionStatus getStatus(){
        return status;
    }
    public void setStatus(AuctionStatus status){
        this.status=status;
    }
    public List<Bid> getBids(){
        return bids;
    }
    public void startAuction(){
        if(status==AuctionStatus.OPEN){
            status=AuctionStatus.RUNNING;
        }
    }
    public boolean isExpired(){
        return LocalDateTime.now().isAfter(endTime);
    }
    public void addBid(Bid bid){
        if(status!=AuctionStatus.RUNNING){
            throw new IllegalStateException("Phien dau gia chua bat dau hoac da ket thuc");
        }
        if(isExpired()){
            status=AuctionStatus.FINISHED;
            throw new IllegalStateException("Phien dau gia da het thoi gian");
        }
        if(bid.getAmount()<currentPrice+bidIncrement){
            throw new IllegalArgumentException("Gia dat toi thieu phai la "+(currentPrice+bidIncrement));
        }
        currentPrice=bid.getAmount();
        highestBidder=bid.getBidder();
        bids.add(bid);
    }
    public Bidder closeAuction(){
        status=AuctionStatus.FINISHED;
        return highestBidder;
    }
}
