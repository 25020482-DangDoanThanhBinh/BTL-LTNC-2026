package network;
import exception.AuthenticationException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import model.Auction;
import model.AuctionStatus;
import model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import storage.DataStorage;
import static org.junit.jupiter.api.Assertions.*;
public class SocketIntegrationTest{
    @Test
    void testMultiClientLoginBidStatusLifecycleAndPersistence(@TempDir Path tempDir)throws Exception{
        Path dataFile=tempDir.resolve("server_data.dat");
        AuctionServer server=new AuctionServer(0,new DataStorage(dataFile));
        server.startAsync();
        int port=server.getLocalPort();
        AuctionClient bidder1Client=new AuctionClient("nguoimua1");
        AuctionClient bidder2Client=new AuctionClient("nguoimua2");
        AuctionClient sellerClient=new AuctionClient("nguoiban");
        try{
            bidder1Client.connect("localhost",port,false);
            assertThrows(AuthenticationException.class,()->bidder1Client.login("nguoimua1","sai_mat_khau"));
            User u1=bidder1Client.login("nguoimua1","1234");
            assertEquals("BIDDER",u1.getRole());
            bidder2Client.connect("localhost",port,false);
            User u2=bidder2Client.login("nguoimua2","1234");
            assertEquals("BIDDER",u2.getRole());
            sellerClient.connect("localhost",port,false);
            User s=sellerClient.login("nguoiban","1234");
            assertEquals("SELLER",s.getRole());
            User regUser=sellerClient.register("sv2026","1234","sv@gmail.com","BIDDER",90000000);
            assertEquals("sv2026",regUser.getUsername());
            CountDownLatch syncLatch=new CountDownLatch(2);
            CountDownLatch bidUpdateLatch=new CountDownLatch(2);
            CountDownLatch errorLatch=new CountDownLatch(1);
            AtomicReference<Double> seenPriceByClient2=new AtomicReference<>(0.0);
            AtomicReference<String> errorMsg=new AtomicReference<>("");
            bidder1Client.setOnMessageReceived(msg->{
                if(msg.getType()==Message.Type.SYNC_STATE){
                    syncLatch.countDown();
                }else if(msg.getType()==Message.Type.UPDATE&&"AUC-101".equals(msg.getAuctionId())){
                    bidUpdateLatch.countDown();
                }else if(msg.getType()==Message.Type.ERROR){
                    errorMsg.set(msg.getContent());
                    errorLatch.countDown();
                }
            });
            bidder2Client.setOnMessageReceived(msg->{
                if(msg.getType()==Message.Type.SYNC_STATE){
                    syncLatch.countDown();
                }else if(msg.getType()==Message.Type.UPDATE&&"AUC-101".equals(msg.getAuctionId())){
                    seenPriceByClient2.set(msg.getAmount());
                    bidUpdateLatch.countDown();
                }
            });
            bidder1Client.join();
            bidder2Client.join();
            assertTrue(syncLatch.await(5,TimeUnit.SECONDS));
            bidder1Client.placeBid("AUC-101",31000000);
            assertTrue(bidUpdateLatch.await(5,TimeUnit.SECONDS));
            assertEquals(31000000.0,seenPriceByClient2.get());
            bidder1Client.placeBid("AUC-101",31100000);
            assertTrue(errorLatch.await(5,TimeUnit.SECONDS));
            assertTrue(errorMsg.get().contains("Gia dat toi thieu"));
            sellerClient.createAuction("nguoiban","ART","Tranh Moi","Mo ta",5000000,"Hoa Si","2025",200000,3);
            List<Auction> all=server.getAuctionService().getAllAuctions();
            assertEquals(5,all.size());
            CountDownLatch startLatch=new CountDownLatch(1);
            bidder2Client.setOnMessageReceived(msg->{
                if(msg.getType()==Message.Type.UPDATE&&"AUC-102".equals(msg.getAuctionId())){
                    startLatch.countDown();
                }
            });
            sellerClient.startAuction("AUC-102");
            assertTrue(startLatch.await(5,TimeUnit.SECONDS));
            assertEquals(AuctionStatus.RUNNING,server.getAuctionService().getAuctionById("AUC-102").getStatus());
            CountDownLatch finishLatch=new CountDownLatch(1);
            bidder2Client.setOnMessageReceived(msg->{
                if(msg.getType()==Message.Type.UPDATE&&"AUC-101".equals(msg.getAuctionId())&&msg.getContent().contains("FINISHED")){
                    finishLatch.countDown();
                }
            });
            sellerClient.finishAuction("AUC-101");
            assertTrue(finishLatch.await(5,TimeUnit.SECONDS));
            assertEquals(AuctionStatus.FINISHED,server.getAuctionService().getAuctionById("AUC-101").getStatus());
            CountDownLatch payLatch=new CountDownLatch(1);
            bidder2Client.setOnMessageReceived(msg->{
                if(msg.getType()==Message.Type.UPDATE&&"AUC-101".equals(msg.getAuctionId())&&msg.getContent().contains("PAID")){
                    payLatch.countDown();
                }
            });
            sellerClient.payAuction("AUC-101");
            assertTrue(payLatch.await(5,TimeUnit.SECONDS));
            assertEquals(AuctionStatus.PAID,server.getAuctionService().getAuctionById("AUC-101").getStatus());
        }finally{
            bidder1Client.disconnect();
            bidder2Client.disconnect();
            sellerClient.disconnect();
            server.stop();
        }
        AuctionServer restarted=new AuctionServer(0,new DataStorage(dataFile));
        try{
            assertEquals(31000000.0,restarted.getAuctionService().getAuctionById("AUC-101").getCurrentPrice());
            assertEquals(AuctionStatus.PAID,restarted.getAuctionService().getAuctionById("AUC-101").getStatus());
            assertEquals(AuctionStatus.RUNNING,restarted.getAuctionService().getAuctionById("AUC-102").getStatus());
            assertEquals(5,restarted.getAuctionService().getAllAuctions().size());
            assertNotNull(restarted.getAuthService().login("sv2026","1234"));
        }finally{
            restarted.stop();
        }
    }
}
