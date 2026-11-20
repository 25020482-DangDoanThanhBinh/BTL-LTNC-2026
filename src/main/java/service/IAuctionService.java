package service;
import exception.AuctionClosedException;
import exception.AuthenticationException;
import exception.InvalidBidException;
import java.time.LocalDateTime;
import java.util.List;
import model.Auction;
import model.Bid;
import model.Bidder;
import model.Seller;
import observer.AuctionObserver;
public interface IAuctionService{
    void addObserver(AuctionObserver observer);
    void removeObserver(AuctionObserver observer);
    Auction createAuction(Seller seller,String itemType,String name,String description,double startingPrice,String attr1,String attr2,double bidIncrement,LocalDateTime endTime)throws AuthenticationException;
    void registerAuction(Auction auction);
    void startAuction(String auctionId)throws AuctionClosedException;
    Bid placeBid(String auctionId,Bidder bidder,double amount)throws InvalidBidException,AuctionClosedException,AuthenticationException;
    Bidder finishAuction(String auctionId)throws AuctionClosedException;
    void markAsPaid(String auctionId)throws AuctionClosedException;
    void cancelAuction(String auctionId)throws AuctionClosedException;
    Auction getAuctionById(String auctionId);
    List<Auction> getAllAuctions();
}
