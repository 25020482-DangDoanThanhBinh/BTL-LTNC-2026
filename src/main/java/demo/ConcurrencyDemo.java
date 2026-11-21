package demo;
import dao.InMemoryAuctionDao;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import model.Auction;
import model.Bidder;
import model.Seller;
import observer.BidConsoleObserver;
import service.AuctionService;
import service.BidValidator;
public class ConcurrencyDemo{
    public static void main(String[] args)throws Exception{
        AuctionService service=new AuctionService(new InMemoryAuctionDao(new ArrayList<>()),new BidValidator());
        service.addObserver(new BidConsoleObserver("ConcurrencyDemo"));
        Seller seller=new Seller("S01","nguoiban","1234","seller@gmail.com");
        Auction auction=service.createAuction(seller,"ELECTRONICS","MacBook Pro M3","Demo da luong",10000000,"Apple","12",500000,LocalDateTime.now().plusHours(1));
        service.startAuction(auction.getAuctionId());
        int threadCount=10;
        ExecutorService pool=Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready=new CountDownLatch(threadCount);
        CountDownLatch start=new CountDownLatch(1);
        CountDownLatch done=new CountDownLatch(threadCount);
        AtomicInteger successCount=new AtomicInteger(0);
        AtomicInteger failCount=new AtomicInteger(0);
        List<Bidder> bidders=new ArrayList<>();
        for(int i=0;i<threadCount;i++){
            bidders.add(new Bidder("B"+i,"bidder"+(i+1),"1234","b"+i+"@gmail.com",50000000));
        }
        for(int i=0;i<threadCount;i++){
            final int idx=i;
            final double bidAmount=10500000+idx*500000;
            pool.submit(()->{
                ready.countDown();
                try{
                    start.await();
                    service.placeBid(auction.getAuctionId(),bidders.get(idx),bidAmount);
                    successCount.incrementAndGet();
                }catch(Exception ex){
                    failCount.incrementAndGet();
                }finally{
                    done.countDown();
                }
            });
        }
        ready.await();
        start.countDown();
        done.await();
        pool.shutdown();
        Bidder winner=service.finishAuction(auction.getAuctionId());
        service.markAsPaid(auction.getAuctionId());
        System.out.println("So luot dat thanh cong: "+successCount.get()+" | Bi tu choi: "+failCount.get());
        System.out.println("Gia cuoi cung: "+String.format("%.0f",auction.getCurrentPrice())+" | Nguoi thang: "+(winner!=null?winner.getUsername():"Khong co"));
        System.out.println("Trang thai cuoi: "+auction.getStatus());
    }
}
