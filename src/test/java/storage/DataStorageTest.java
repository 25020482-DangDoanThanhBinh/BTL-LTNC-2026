package storage;
import dao.InMemoryAuctionDao;
import dao.InMemoryUserDao;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import model.Auction;
import model.AuctionStatus;
import model.Bidder;
import model.Item;
import model.Seller;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import service.AuctionService;
import service.BidValidator;
import static org.junit.jupiter.api.Assertions.*;
public class DataStorageTest{
    @Test
    void testSaveAndLoadPreservesUsersItemsAuctionsAndBids(@TempDir Path tempDir)throws Exception{
        Path dataFile=tempDir.resolve("auction_test.dat");
        DataStorage storage=new DataStorage(dataFile);
        assertNull(storage.load());
        InMemoryUserDao userDao=new InMemoryUserDao(new ArrayList<>());
        InMemoryAuctionDao auctionDao=new InMemoryAuctionDao(new ArrayList<>());
        AuctionService auctionService=new AuctionService(auctionDao,new BidValidator());
        Seller seller=new Seller("U01","nguoiban","1234","seller@gmail.com");
        Bidder bidder=new Bidder("U02","nguoimua1","1234","b1@gmail.com",50000000);
        userDao.save(seller);
        userDao.save(bidder);
        Auction auction=auctionService.createAuction(seller,"ELECTRONICS","MacBook Pro","May dep",20000000,"Apple","12",500000,LocalDateTime.now().plusHours(2));
        auctionService.startAuction(auction.getAuctionId());
        auctionService.placeBid(auction.getAuctionId(),bidder,21000000);
        List<Item> items=new ArrayList<>();
        items.add(auction.getItem());
        storage.save(userDao.findAll(),items,auctionDao.findAll());
        assertTrue(storage.exists());
        AuctionDataSnapshot loaded=storage.load();
        assertNotNull(loaded);
        assertEquals(2,loaded.getUsers().size());
        assertEquals(1,loaded.getItems().size());
        assertEquals(1,loaded.getAuctions().size());
        Auction loadedAuction=loaded.getAuctions().get(0);
        assertEquals(auction.getAuctionId(),loadedAuction.getAuctionId());
        assertEquals(21000000,loadedAuction.getCurrentPrice());
        assertEquals(AuctionStatus.RUNNING,loadedAuction.getStatus());
        assertNotNull(loadedAuction.getHighestBidder());
        assertEquals("nguoimua1",loadedAuction.getHighestBidder().getUsername());
        assertEquals(1,loadedAuction.getBids().size());
    }
}
