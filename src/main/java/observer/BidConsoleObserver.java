package observer;
public class BidConsoleObserver implements AuctionObserver{
    private final String listenerName;
    public BidConsoleObserver(String listenerName){
        this.listenerName=listenerName;
    }
    @Override
    public void onNewBid(String auctionId,String bidderName,double newAmount){
        System.out.printf("[%s] Phien %s co gia moi: %,.0f VND (boi %s)%n",listenerName,auctionId,newAmount,bidderName);
    }
    @Override
    public void onStatusChanged(String auctionId,String newStatus){
        System.out.printf("[%s] Phien %s da chuyen trang thai -> %s%n",listenerName,auctionId,newStatus);
    }
}
