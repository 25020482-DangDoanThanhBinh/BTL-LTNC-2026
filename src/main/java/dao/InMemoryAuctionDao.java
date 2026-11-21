package dao;

import manager.AuctionManager;
import model.Auction;

import java.util.ArrayList;
import java.util.List;

public class InMemoryAuctionDao implements AuctionDao{
    private final List<Auction> auctions;
    public InMemoryAuctionDao(){
        this.auctions=AuctionManager.getInstance().getAuctions();
    }
    public InMemoryAuctionDao(List<Auction> auctions){
        this.auctions=auctions;
    }
    @Override
    public void update(Auction auction){
        if(auction==null){
            return;
        }
        synchronized(auctions){
            for(int i=0;i<auctions.size();i++){
                if(auctions.get(i).getAuctionId().equals(auction.getAuctionId())){
                    auctions.set(i,auction);
                    return;
                }
            }
        }
    }
    @Override
    public void delete(String auctionId){
        if(auctionId==null){
            return;
        }
        synchronized(auctions){
            auctions.removeIf(a->a.getAuctionId().equals(auctionId));
        }
    }
    @Override
    public void save(Auction auction){
        synchronized(auctions){
            auctions.add(auction);
        }
    }
    @Override
    public Auction findById(String auctionId){
        if(auctionId==null){
            return null;
        }
        synchronized(auctions){
            for(int i=0;i<auctions.size();i++){
                if(auctions.get(i).getAuctionId().equals(auctionId)){
                    return auctions.get(i);
                }
            }
        }
        return null;
    }
    @Override
    public List<Auction> findAll(){
        synchronized(auctions){
            return new ArrayList<>(auctions);
        }
    }
    @Override
    public void clear(){
        synchronized(auctions){
            auctions.clear();
        }
    }
}
