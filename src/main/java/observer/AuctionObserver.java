package observer;
public interface AuctionObserver{
    void onNewBid(String auctionId,String bidderName,double newAmount);
    void onStatusChanged(String auctionId,String newStatus);
}
