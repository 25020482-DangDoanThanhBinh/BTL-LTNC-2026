package service;

import model.AuctionStatus;
import observer.AuctionObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionEngine {
    private final String auctionId;
    private final String itemName;
    private double currentPrice;
    private final double bidIncrement;
    private String highestBidder = "Chưa có";
    private AuctionStatus status;

    private final List<AuctionObserver> observers = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    public AuctionEngine(String auctionId, String itemName, double startingPrice, double bidIncrement) {
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.currentPrice = startingPrice;
        this.bidIncrement = bidIncrement;
        this.status = AuctionStatus.OPEN;
    }

    public void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    public void startAuction() {
        lock.lock();
        try {
            if (status == AuctionStatus.OPEN) {
                status = AuctionStatus.RUNNING;
                notifyStatusChanged();
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean placeBid(String bidderName, double amount) {
        lock.lock();
        try {
            if (status != AuctionStatus.RUNNING) {
                return false;
            }
            if (amount < currentPrice + bidIncrement) {
                return false;
            }

            this.currentPrice = amount;
            this.highestBidder = bidderName;

            for (AuctionObserver obs : observers) {
                obs.onNewBid(auctionId, bidderName, amount);
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void finishAuction() {
        lock.lock();
        try {
            if (status == AuctionStatus.RUNNING) {
                status = AuctionStatus.FINISHED;
                notifyStatusChanged();
            }
        } finally {
            lock.unlock();
        }
    }

    private void notifyStatusChanged() {
        for (AuctionObserver obs : observers) {
            obs.onStatusChanged(auctionId, status.name());
        }
    }

    public String getAuctionId() { return auctionId; }
    public String getItemName() { return itemName; }
    public double getCurrentPrice() { return currentPrice; }
    public double getBidIncrement() { return bidIncrement; }
    public String getHighestBidder() { return highestBidder; }
    public AuctionStatus getStatus() { return status; }
}