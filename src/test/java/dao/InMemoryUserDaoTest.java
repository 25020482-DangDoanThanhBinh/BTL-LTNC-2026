package dao;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import java.util.ArrayList;
import model.Bidder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
public class InMemoryUserDaoTest{
    private InMemoryUserDao dao;
    @BeforeEach
    void setUp(){
        dao=new InMemoryUserDao(new ArrayList<>());
    }
    @Test
    void testUpdateReplacesUserWithSameId(){
        dao.save(new Bidder("B01","bidder1","1234","b1@gmail.com",1000000));
        Bidder updated=new Bidder("B01","bidderNew","1234","new@gmail.com",5000000);
        dao.update(updated);
        assertSame(updated,dao.findById("B01"));
        assertEquals(1,dao.findAll().size());
    }
    @Test
    void testDeleteRemovesUser(){
        dao.save(new Bidder("B01","bidder1","1234","b1@gmail.com",1000000));
        dao.delete("B01");
        assertNull(dao.findById("B01"));
        assertNull(dao.findByUsername("bidder1"));
    }
    @Test
    void testDeleteUnknownIdKeepsOtherUsers(){
        dao.save(new Bidder("B01","bidder1","1234","b1@gmail.com",1000000));
        dao.delete("B99");
        assertEquals(1,dao.findAll().size());
    }
}