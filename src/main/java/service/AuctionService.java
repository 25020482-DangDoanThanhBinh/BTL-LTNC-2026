package service;
import dao.AuctionDao;
import exception.AuctionClosedException;
import exception.AuthenticationException;
import exception.InvalidBidException;
import factory.ItemFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import manager.AuctionManager;
import model.Auction;
import model.AuctionStatus;
import model.Bid;
import model.Bidder;
import model.Item;
import model.Seller;
import observer.AuctionObserver;
public class AuctionService implements IAuctionService{
    private final AuctionDao auctionDao;
    private final BidValidator bidValidator;
    private final ConcurrentHashMap<String,ReentrantLock> auctionLocks;
    private final CopyOnWriteArrayList<AuctionObserver> observers;
    private final AtomicInteger itemSeq;
    private final AtomicInteger auctionSeq;
    public AuctionService(AuctionDao auctionDao,BidValidator bidValidator){
        this.auctionDao=auctionDao;
        this.bidValidator=bidValidator;
        this.auctionLocks=new ConcurrentHashMap<>();
        this.observers=new CopyOnWriteArrayList<>();
        this.itemSeq=new AtomicInteger(100);
        this.auctionSeq=new AtomicInteger(100);
    }
    private ReentrantLock getLock(String auctionId){
        return auctionLocks.computeIfAbsent(auctionId,k->new ReentrantLock(true));
    }
    @Override
    public void addObserver(AuctionObserver observer){
        if(observer!=null&&!observers.contains(observer)){
            observers.add(observer);
        }
    }
    @Override
    public void removeObserver(AuctionObserver observer){
        observers.remove(observer);
    }
    @Override
    public Auction createAuction(Seller seller,String itemType,String name,String description,double startingPrice,String attr1,String attr2,double bidIncrement,LocalDateTime endTime)throws AuthenticationException{
        if(seller==null||!seller.isActive()){
            throw new AuthenticationException("Tai khoan nguoi ban khong hop le hoac bi khoa");
        }
        if(name==null||name.trim().isEmpty()){
            throw new IllegalArgumentException("Ten san pham khong duoc de trong");
        }
        if(endTime==null||!endTime.isAfter(LocalDateTime.now())){
            throw new IllegalArgumentException("Thoi gian ket thuc phai o tuong lai");
        }
        String itemId="ITEM-"+itemSeq.incrementAndGet();
        Item item=ItemFactory.createItem(itemType,itemId,name.trim(),description==null?"":description.trim(),startingPrice,seller.getUserId(),attr1,attr2);
        seller.addItem(item);
        synchronized(AuctionManager.getInstance()){
            AuctionManager.getInstance().addItem(item);
        }
        String auctionId="AUC-"+auctionSeq.incrementAndGet();
        Auction auction=new Auction(auctionId,item,seller,bidIncrement,LocalDateTime.now(),endTime);
        seller.getCreatedAuctions().add(auction);
        auctionDao.save(auction);
        return auction;
    }
    @Override
    public void registerAuction(Auction auction){
        if(auction==null){
            throw new IllegalArgumentException("Phien dau gia khong duoc null");
        }
        auctionDao.save(auction);
    }
    @Override
    public void startAuction(String auctionId)throws AuctionClosedException{
        ReentrantLock lock=getLock(auctionId);
        lock.lock();
        try{
            Auction auction=requireAuction(auctionId);
            if(auction.getStatus()!=AuctionStatus.OPEN){
                throw new AuctionClosedException("Chi co the bat dau phien o trang thai OPEN");
            }
            auction.startAuction();
            notifyStatusChanged(auctionId,auction.getStatus().name());
        }finally{
            lock.unlock();
        }
    }
    @Override
    public Bid placeBid(String auctionId,Bidder bidder,double amount)throws InvalidBidException,AuctionClosedException,AuthenticationException{
        ReentrantLock lock=getLock(auctionId);
        lock.lock();
        try{
            Auction auction=requireAuction(auctionId);
            if(auction.getStatus()==AuctionStatus.RUNNING&&auction.isExpired()){
                auction.closeAuction();
                notifyStatusChanged(auctionId,auction.getStatus().name());
                throw new AuctionClosedException("Phien dau gia da het thoi gian");
            }
            bidValidator.validateBid(auction,bidder,amount);
            Bid bid=bidder.placeBid(auction,amount);
            notifyNewBid(auctionId,bidder.getUsername(),amount);
            return bid;
        }finally{
            lock.unlock();
        }
    }
    @Override
    public Bidder finishAuction(String auctionId)throws AuctionClosedException{
        ReentrantLock lock=getLock(auctionId);
        lock.lock();
        try{
            Auction auction=requireAuction(auctionId);
            if(auction.getStatus()!=AuctionStatus.RUNNING){
                throw new AuctionClosedException("Chi co the ket thuc phien dang RUNNING");
            }
            Bidder winner=auction.closeAuction();
            notifyStatusChanged(auctionId,auction.getStatus().name());
            return winner;
        }finally{
            lock.unlock();
        }
    }
    @Override
    public void markAsPaid(String auctionId)throws AuctionClosedException{
        ReentrantLock lock=getLock(auctionId);
        lock.lock();
        try{
            Auction auction=requireAuction(auctionId);
            if(auction.getStatus()!=AuctionStatus.FINISHED){
                throw new AuctionClosedException("Chi co the thanh toan phien da FINISHED");
            }
            if(auction.getHighestBidder()==null){
                throw new AuctionClosedException("Phien khong co nguoi thang de thanh toan");
            }
            auction.setStatus(AuctionStatus.PAID);
            notifyStatusChanged(auctionId,auction.getStatus().name());
        }finally{
            lock.unlock();
        }
    }
    @Override
    public void cancelAuction(String auctionId)throws AuctionClosedException{
        ReentrantLock lock=getLock(auctionId);
        lock.lock();
        try{
            Auction auction=requireAuction(auctionId);
            if(auction.getStatus()!=AuctionStatus.OPEN&&auction.getStatus()!=AuctionStatus.RUNNING){
                throw new AuctionClosedException("Khong the huy phien o trang thai "+auction.getStatus());
            }
            auction.setStatus(AuctionStatus.CANCELED);
            notifyStatusChanged(auctionId,auction.getStatus().name());
        }finally{
            lock.unlock();
        }
    }
    @Override
    public Auction getAuctionById(String auctionId){
        return auctionDao.findById(auctionId);
    }
    @Override
    public List<Auction> getAllAuctions(){
        return auctionDao.findAll();
    }
    private Auction requireAuction(String auctionId)throws AuctionClosedException{
        Auction auction=auctionDao.findById(auctionId);
        if(auction==null){
            throw new AuctionClosedException("Khong tim thay phien dau gia: "+auctionId);
        }
        return auction;
    }
    private void notifyNewBid(String auctionId,String bidderName,double amount){
        for(int i=0;i<observers.size();i++){
            observers.get(i).onNewBid(auctionId,bidderName,amount);
        }
    }
    private void notifyStatusChanged(String auctionId,String newStatus){
        for(int i=0;i<observers.size();i++){
            observers.get(i).onStatusChanged(auctionId,newStatus);
        }
    }
}
