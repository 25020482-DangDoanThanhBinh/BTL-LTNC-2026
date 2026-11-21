package dao;
import java.util.List;
import model.Auction;
public interface AuctionDao{
    void save(Auction auction);
    Auction findById(String auctionId);
    List<Auction> findAll();
    void clear();
}
