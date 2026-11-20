package service;
import dao.InMemoryUserDao;
import exception.AuthenticationException;
import java.util.ArrayList;
import model.Admin;
import model.Bidder;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class AuthServiceTest{
    private AuthService authService;
    private InMemoryUserDao userDao;
    private Bidder bidder;
    @BeforeEach
    void setUp(){
        userDao=new InMemoryUserDao(new ArrayList<>());
        authService=new AuthService(userDao);
        bidder=new Bidder("U01","nguoimua1","1234","b1@gmail.com",10000000);
        userDao.save(bidder);
    }
    @Test
    void testLoginSuccess()throws Exception{
        User logged=authService.login("nguoimua1","1234");
        assertEquals("U01",logged.getUserId());
        assertEquals("BIDDER",logged.getRole());
    }
    @Test
    void testLoginWrongPasswordThrowsAuthenticationException(){
        assertThrows(AuthenticationException.class,()->authService.login("nguoimua1","9999"));
    }
    @Test
    void testLoginNonExistentUserThrowsAuthenticationException(){
        assertThrows(AuthenticationException.class,()->authService.login("khongtontai","1234"));
    }
    @Test
    void testLoginBannedUserThrowsAuthenticationException(){
        Admin admin=new Admin("A01","admin","1234","admin@gmail.com");
        admin.banUser(bidder);
        assertThrows(AuthenticationException.class,()->authService.login("nguoimua1","1234"));
    }
    @Test
    void testRegisterSuccessAndDuplicateThrowsAuthenticationException()throws Exception{
        User created=authService.register("newuser","abcd","new@gmail.com","SELLER",0);
        assertEquals("SELLER",created.getRole());
        assertThrows(AuthenticationException.class,()->authService.register("newuser","abcd","other@gmail.com","BIDDER",1000));
    }
}
