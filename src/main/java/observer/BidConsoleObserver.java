package observer;

public class BidConsoleObserver implements AuctionObserver {
    private final String listenerName;

    public BidConsoleObserver(String listenerName) {
        this.listenerName = listenerName;
    }

    @Override
    public void onNewBid(String auctionId, String bidderName, double newAmount) {
        System.out.printf("[%s] Phiên %s có giá mới: %,.0f VND (bởi %s)%n",
                listenerName, auctionId, newAmount, bidderName);
    }

    @Override
    public void onStatusChanged(String auctionId, String newStatus) {
        System.out.printf("[%s] Phiên %s đã chuyển trạng thái -> %s%n",
                listenerName, auctionId, newStatus);
    }
}