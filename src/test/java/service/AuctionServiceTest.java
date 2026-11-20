package service;
import dao.InMemoryAuctionDao;
import exception.AuctionClosedException;
import exception.AuthenticationException;
import exception.InvalidBidException;
import factory.ItemFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import model.Auction;
import model.AuctionStatus;
import model.Bid;
import model.Bidder;
import model.Item;
import model.Seller;
import observer.AuctionObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class AuctionServiceTest{
    private AuctionService auctionService;
    private Seller seller;
    private Bidder bidder1;
    private Bidder bidder2;
    private Auction auction;
    @BeforeEach
    void setUp()throws Exception{
        auctionService=new AuctionService(new InMemoryAuctionDao(new ArrayList<>()),new BidValidator());
        seller=new Seller("S01","seller1","1234","s1@gmail.com");
        bidder1=new Bidder("B01","bidder1","1234","b1@gmail.com",50000000);
        bidder2=new Bidder("B02","bidder2","1234","b2@gmail.com",80000000);
        auction=auctionService.createAuction(seller,"ELECTRONICS","MacBook Pro","Moi 99%",15000000,"Apple","12",500000,LocalDateTime.now().plusHours(2));
    }
    @Test
    void testPlaceValidBidUpdatesPriceAndHighestBidder()throws Exception{
        auctionService.startAuction(auction.getAuctionId());
        Bid bid=auctionService.placeBid(auction.getAuctionId(),bidder1,16000000);
        assertNotNull(bid);
        assertEquals(16000000,auction.getCurrentPrice());
        assertEquals(bidder1,auction.getHighestBidder());
        assertEquals(1,auction.getBids().size());
    }
    @Test
    void testPlaceBidLowerThanMinimumThrowsInvalidBidException()throws Exception{
        auctionService.startAuction(auction.getAuctionId());
        assertThrows(InvalidBidException.class,()->auctionService.placeBid(auction.getAuctionId(),bidder1,15200000));
    }
    @Test
    void testPlaceBidExceedingBalanceThrowsInvalidBidException()throws Exception{
        auctionService.startAuction(auction.getAuctionId());
        assertThrows(InvalidBidException.class,()->auctionService.placeBid(auction.getAuctionId(),bidder1,60000000));
    }
    @Test
    void testPlaceBidOnOpenAuctionThrowsAuctionClosedException(){
        assertThrows(AuctionClosedException.class,()->auctionService.placeBid(auction.getAuctionId(),bidder1,16000000));
    }
    @Test
    void testPlaceBidOnFinishedAuctionThrowsAuctionClosedException()throws Exception{
        auctionService.startAuction(auction.getAuctionId());
        auctionService.finishAuction(auction.getAuctionId());
        assertThrows(AuctionClosedException.class,()->auctionService.placeBid(auction.getAuctionId(),bidder1,16000000));
    }
    @Test
    void testPlaceBidOnExpiredAuctionClosesAndThrowsAuctionClosedException()throws Exception{
        Item item=ItemFactory.createItem("ART","I99","Tranh Co","Mo ta",5000000,seller.getUserId(),"Tac Gia","1950");
        Auction expiredAuction=new Auction("AUC-EXP",item,seller,200000,LocalDateTime.now().minusHours(2),LocalDateTime.now().minusSeconds(1));
        auctionService.registerAuction(expiredAuction);
        auctionService.startAuction(expiredAuction.getAuctionId());
        assertThrows(AuctionClosedException.class,()->auctionService.placeBid(expiredAuction.getAuctionId(),bidder1,6000000));
        assertEquals(AuctionStatus.FINISHED,expiredAuction.getStatus());
    }
    @Test
    void testPlaceBidByBannedBidderThrowsAuthenticationException()throws Exception{
        auctionService.startAuction(auction.getAuctionId());
        bidder1.setActive(false);
        assertThrows(AuthenticationException.class,()->auctionService.placeBid(auction.getAuctionId(),bidder1,16000000));
    }
    @Test
    void testFullStateTransitionOpenRunningFinishedPaid()throws Exception{
        assertEquals(AuctionStatus.OPEN,auction.getStatus());
        auctionService.startAuction(auction.getAuctionId());
        assertEquals(AuctionStatus.RUNNING,auction.getStatus());
        auctionService.placeBid(auction.getAuctionId(),bidder1,16000000);
        auctionService.placeBid(auction.getAuctionId(),bidder2,17500000);
        Bidder winner=auctionService.finishAuction(auction.getAuctionId());
        assertEquals(AuctionStatus.FINISHED,auction.getStatus());
        assertEquals(bidder2,winner);
        auctionService.markAsPaid(auction.getAuctionId());
        assertEquals(AuctionStatus.PAID,auction.getStatus());
    }
    @Test
    void testCancelAuctionFromRunningAndRejectInvalidTransitions()throws Exception{
        auctionService.startAuction(auction.getAuctionId());
        auctionService.cancelAuction(auction.getAuctionId());
        assertEquals(AuctionStatus.CANCELED,auction.getStatus());
        assertThrows(AuctionClosedException.class,()->auctionService.startAuction(auction.getAuctionId()));
        assertThrows(AuctionClosedException.class,()->auctionService.markAsPaid(auction.getAuctionId()));
    }
    @Test
    void testObserverReceivesBidAndStatusNotifications()throws Exception{
        AtomicInteger bidEvents=new AtomicInteger(0);
        AtomicInteger statusEvents=new AtomicInteger(0);
        auctionService.addObserver(new AuctionObserver(){
            @Override
            public void onNewBid(String auctionId,String bidderName,double newAmount){
                bidEvents.incrementAndGet();
            }
            @Override
            public void onStatusChanged(String auctionId,String newStatus){
                statusEvents.incrementAndGet();
            }
        });
        auctionService.startAuction(auction.getAuctionId());
        auctionService.placeBid(auction.getAuctionId(),bidder1,16000000);
        auctionService.finishAuction(auction.getAuctionId());
        assertEquals(1,bidEvents.get());
        assertEquals(2,statusEvents.get());
    }
    @Test
    void testConcurrentBiddingHasNoLostUpdateOrRaceCondition()throws Exception{
        auctionService.startAuction(auction.getAuctionId());
        int threads=20;
        ExecutorService pool=Executors.newFixedThreadPool(threads);
        CountDownLatch ready=new CountDownLatch(threads);
        CountDownLatch start=new CountDownLatch(1);
        CountDownLatch done=new CountDownLatch(threads);
        List<Bidder> concurrentBidders=new ArrayList<>();
        for(int i=0;i<threads;i++){
            concurrentBidders.add(new Bidder("CB"+i,"user"+i,"1234","u"+i+"@gmail.com",100000000));
        }
        double expectedMax=16000000+(threads-1)*500000;
        for(int i=0;i<threads;i++){
            final int idx=i;
            final double amount=16000000+idx*500000;
            pool.submit(()->{
                ready.countDown();
                try{
                    start.await();
                    auctionService.placeBid(auction.getAuctionId(),concurrentBidders.get(idx),amount);
                }catch(Exception ignored){
                }finally{
                    done.countDown();
                }
            });
        }
        ready.await();
        start.countDown();
        done.await();
        pool.shutdown();
        assertEquals(expectedMax,auction.getCurrentPrice());
        assertEquals("user"+(threads-1),auction.getHighestBidder().getUsername());
        List<Bid> bids=auction.getBids();
        for(int i=1;i<bids.size();i++){
            assertTrue(bids.get(i).getAmount()>=bids.get(i-1).getAmount()+auction.getBidIncrement());
        }
    }
}
