package service;
import dao.AuctionDao;
import dao.InMemoryAuctionDao;
import dao.InMemoryUserDao;
import dao.UserDao;
import java.time.LocalDateTime;
import model.Admin;
import model.Auction;
import model.Bidder;
import model.Seller;
import model.User;
import observer.BidConsoleObserver;
public class AppContext{
    private static volatile AppContext instance;
    private final UserDao userDao;
    private final AuctionDao auctionDao;
    private final IAuthService authService;
    private final IAuctionService auctionService;
    private User currentUser;
    private boolean seeded;
    private AppContext(){
        this.userDao=new InMemoryUserDao();
        this.auctionDao=new InMemoryAuctionDao();
        this.authService=new AuthService(userDao);
        this.auctionService=new AuctionService(auctionDao,new BidValidator());
        this.auctionService.addObserver(new BidConsoleObserver("System"));
        seedSampleData();
    }
    public static AppContext getInstance(){
        if(instance==null){
            synchronized(AppContext.class){
                if(instance==null){
                    instance=new AppContext();
                }
            }
        }
        return instance;
    }
    private void seedSampleData(){
        if(seeded||!userDao.findAll().isEmpty()){
            seeded=true;
            return;
        }
        Seller seller=new Seller("U01","nguoiban","1234","seller@gmail.com");
        Bidder bidder1=new Bidder("U02","nguoimua1","1234","b1@gmail.com",100000000);
        Bidder bidder2=new Bidder("U03","nguoimua2","1234","b2@gmail.com",150000000);
        Admin admin=new Admin("U04","admin","1234","admin@gmail.com");
        userDao.save(seller);
        userDao.save(bidder1);
        userDao.save(bidder2);
        userDao.save(admin);
        try{
            Auction a1=auctionService.createAuction(seller,"ELECTRONICS","MacBook Pro M3","May dep 99%",30000000,"Apple","12",500000,LocalDateTime.now().plusHours(4));
            Auction a2=auctionService.createAuction(seller,"ART","Tranh Son Dau Co","Tranh phong canh Viet Nam",15000000,"To Ngoc Van","1960",500000,LocalDateTime.now().plusHours(6));
            Auction a3=auctionService.createAuction(seller,"VEHICLE","Honda SH 160i","Xe di it, chinh chu",45000000,"Honda","3500",1000000,LocalDateTime.now().plusHours(3));
            Auction a4=auctionService.createAuction(seller,"ELECTRONICS","Apple Watch Ultra","Fullbox kem day deo",12000000,"Apple","6",300000,LocalDateTime.now().plusHours(2));
            auctionService.startAuction(a1.getAuctionId());
            auctionService.startAuction(a3.getAuctionId());
            auctionService.startAuction(a4.getAuctionId());
            auctionService.placeBid(a4.getAuctionId(),bidder1,12500000);
            auctionService.finishAuction(a4.getAuctionId());
            if(a2==null){
                return;
            }
        }catch(Exception ignored){
        }
        seeded=true;
    }
    public IAuthService getAuthService(){
        return authService;
    }
    public IAuctionService getAuctionService(){
        return auctionService;
    }
    public User getCurrentUser(){
        return currentUser;
    }
    public void setCurrentUser(User currentUser){
        this.currentUser=currentUser;
    }
    public Seller findDefaultSeller(){
        for(User u:userDao.findAll()){
            if(u instanceof Seller){
                return(Seller)u;
            }
        }
        return null;
    }
}
