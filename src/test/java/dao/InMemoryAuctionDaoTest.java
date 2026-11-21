package dao;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import factory.ItemFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import model.Auction;
import model.Item;
import model.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
public class InMemoryAuctionDaoTest{
    private InMemoryAuctionDao dao;
    private Seller seller;
    @BeforeEach
    void setUp(){
        dao=new InMemoryAuctionDao(new ArrayList<>());
        seller=new Seller("S01","seller1","1234","s1@gmail.com");
    }
    private Auction newAuction(String auctionId,double startingPrice){
        Item item=ItemFactory.createItem("ART","I01","Tranh Co","Mo ta",startingPrice,seller.getUserId(),"Tac Gia","1950");
        return new Auction(auctionId,item,seller,200000,LocalDateTime.now(),LocalDateTime.now().plusHours(2));
    }
    @Test
    void testSaveAndFindById(){
        Auction auction=newAuction("A01",1000000);
        dao.save(auction);
        assertNotNull(dao.findById("A01"));
    }
    @Test
    void testUpdateReplacesAuctionWithSameId(){
        dao.save(newAuction("A01",1000000));
        Auction updated=newAuction("A01",2000000);
        dao.update(updated);
        assertSame(updated,dao.findById("A01"));
        assertEquals(1,dao.findAll().size());
    }
    @Test
    void testUpdateUnknownIdDoesNothing(){
        dao.save(newAuction("A01",1000000));
        dao.update(newAuction("A99",2000000));
        assertNull(dao.findById("A99"));
        assertEquals(1,dao.findAll().size());
    }
    @Test
    void testDeleteRemovesAuction(){
        dao.save(newAuction("A01",1000000));
        dao.delete("A01");
        assertNull(dao.findById("A01"));
        assertEquals(0,dao.findAll().size());
    }
    @Test
    void testDeleteNullDoesNothing(){
        dao.save(newAuction("A01",1000000));
        dao.delete(null);
        assertEquals(1,dao.findAll().size());
    }
}