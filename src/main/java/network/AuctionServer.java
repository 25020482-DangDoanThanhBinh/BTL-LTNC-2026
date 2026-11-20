package network;
import dao.InMemoryAuctionDao;
import dao.InMemoryUserDao;
import exception.AuctionClosedException;
import exception.AuthenticationException;
import exception.InvalidBidException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import model.Admin;
import model.Auction;
import model.Bidder;
import model.Item;
import model.Seller;
import model.User;
import observer.BidConsoleObserver;
import service.AuctionService;
import service.AuthService;
import service.BidValidator;
import service.IAuctionService;
import service.IAuthService;
import storage.AuctionDataSnapshot;
import storage.DataStorage;
public class AuctionServer{
    public static final int PORT=8888;
    private final int port;
    private final DataStorage storage;
    private final InMemoryUserDao userDao;
    private final InMemoryAuctionDao auctionDao;
    private final AuthService authService;
    private final AuctionService auctionService;
    private final List<ClientHandler> clients;
    private final ReentrantLock stateLock;
    private final ExecutorService pool;
    private ServerSocket serverSocket;
    private volatile boolean running;
    public AuctionServer(){
        this(PORT,new DataStorage());
    }
    public AuctionServer(int port,DataStorage storage){
        this.port=port;
        this.storage=storage;
        this.userDao=new InMemoryUserDao(new ArrayList<>());
        this.auctionDao=new InMemoryAuctionDao(new ArrayList<>());
        this.authService=new AuthService(userDao);
        this.auctionService=new AuctionService(auctionDao,new BidValidator());
        this.auctionService.addObserver(new BidConsoleObserver("Server"));
        this.clients=new CopyOnWriteArrayList<>();
        this.stateLock=new ReentrantLock(true);
        this.pool=Executors.newCachedThreadPool();
        loadOrSeedData();
    }
    private void loadOrSeedData(){
        stateLock.lock();
        try{
            AuctionDataSnapshot snapshot=null;
            try{
                snapshot=storage.load();
            }catch(IOException|ClassNotFoundException ex){
                System.err.println("[Server] Khong the doc file luu tru, khoi tao du lieu mau: "+ex.getMessage());
            }
            if(snapshot!=null&&!snapshot.getUsers().isEmpty()){
                List<User> loadedUsers=snapshot.getUsers();
                for(int i=0;i<loadedUsers.size();i++){
                    userDao.save(loadedUsers.get(i));
                }
                List<Auction> loadedAuctions=snapshot.getAuctions();
                for(int i=0;i<loadedAuctions.size();i++){
                    auctionDao.save(loadedAuctions.get(i));
                }
                authService.syncIdSequence();
                auctionService.syncIdSequences();
                System.out.println("[Server] Da tai du lieu tu "+storage.getFilePath()+" ("+loadedUsers.size()+" users, "+loadedAuctions.size()+" phien).");
                return;
            }
            seedSampleData();
            persistState();
        }finally{
            stateLock.unlock();
        }
    }
    private void seedSampleData(){
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
        }catch(Exception ex){
            System.err.println("[Server] Loi khoi tao du lieu mau: "+ex.getMessage());
        }
    }
    public void start()throws IOException{
        openServerSocket();
        acceptLoop();
    }
    public void startAsync()throws IOException{
        openServerSocket();
        Thread acceptThread=new Thread(this::acceptLoop);
        acceptThread.setDaemon(true);
        acceptThread.start();
    }
    private void openServerSocket()throws IOException{
        serverSocket=new ServerSocket(port);
        running=true;
        System.out.println("=== AUCTION SERVER DANG CHAY TAI CONG "+serverSocket.getLocalPort()+" ===");
    }
    private void acceptLoop(){
        try{
            while(running){
                Socket clientSocket=serverSocket.accept();
                System.out.println("[Server] Client moi ket noi: "+clientSocket.getRemoteSocketAddress());
                ClientHandler handler=new ClientHandler(clientSocket,this);
                clients.add(handler);
                pool.execute(handler);
            }
        }catch(IOException ex){
            if(running){
                System.err.println("[Server] Loi lang nghe ket noi: "+ex.getMessage());
            }
        }
    }
    public void stop(){
        running=false;
        stateLock.lock();
        try{
            persistState();
        }finally{
            stateLock.unlock();
        }
        List<ClientHandler> active=new ArrayList<>(clients);
        for(int i=0;i<active.size();i++){
            active.get(i).close();
        }
        clients.clear();
        if(serverSocket!=null&&!serverSocket.isClosed()){
            try{
                serverSocket.close();
            }catch(IOException ignored){
            }
        }
        pool.shutdownNow();
    }
    public int getLocalPort(){
        return serverSocket!=null?serverSocket.getLocalPort():port;
    }
    public int getClientCount(){
        return clients.size();
    }
    public IAuctionService getAuctionService(){
        return auctionService;
    }
    public IAuthService getAuthService(){
        return authService;
    }
    public void handleClientMessage(Message msg,ClientHandler handler){
        switch(msg.getType()){
            case LOGIN->processLogin(msg,handler);
            case REGISTER->processRegister(msg,handler);
            case JOIN->processJoin(msg,handler);
            case CREATE_AUCTION->processCreateAuction(msg,handler);
            case BID->processBid(msg,handler);
            case START_AUCTION->processStartAuction(msg,handler);
            case FINISH_AUCTION->processFinishAuction(msg,handler);
            case PAY_AUCTION->processPayAuction(msg,handler);
            case CANCEL_AUCTION->processCancelAuction(msg,handler);
            default->handler.sendMessage(new Message(Message.Type.ERROR,msg.getRequestId(),msg.getAuctionId(),"SERVER",0,"Loai yeu cau khong duoc ho tro: "+msg.getType(),null));
        }
    }
    private void processLogin(Message msg,ClientHandler handler){
        stateLock.lock();
        try{
            String[] creds=(String[])msg.getPayload();
            if(creds==null||creds.length<2){
                handler.sendMessage(new Message(Message.Type.ERROR,msg.getRequestId(),"","SERVER",0,"Thieu thong tin dang nhap",null));
                return;
            }
            User user=authService.login(creds[0],creds[1]);
            handler.setUsername(user.getUsername());
            handler.sendMessage(new Message(Message.Type.LOGIN_RESULT,msg.getRequestId(),"","SERVER",0,"Dang nhap thanh cong",user));
        }catch(AuthenticationException ex){
            handler.sendMessage(new Message(Message.Type.ERROR,msg.getRequestId(),"","SERVER",0,ex.getMessage(),null));
        }catch(ClassCastException ex){
            handler.sendMessage(new Message(Message.Type.ERROR,msg.getRequestId(),"","SERVER",0,"Du lieu dang nhap khong hop le",null));
        }finally{
            stateLock.unlock();
        }
    }
    private void processRegister(Message msg,ClientHandler handler){
        stateLock.lock();
        try{
            String[] params=(String[])msg.getPayload();
            if(params==null||params.length<5){
                handler.sendMessage(new Message(Message.Type.ERROR,msg.getRequestId(),"","SERVER",0,"Thieu thong tin dang ky",null));
                return;
            }
            double initialBalance=Double.parseDouble(params[4]);
            User created=authService.register(params[0],params[1],params[2],params[3],initialBalance);
            persistState();
            handler.sendMessage(new Message(Message.Type.REGISTER_RESULT,msg.getRequestId(),"","SERVER",0,"Dang ky thanh cong",created));
        }catch(NumberFormatException ex){
            handler.sendMessage(new Message(Message.Type.ERROR,msg.getRequestId(),"","SERVER",0,"So du ban dau khong hop le",null));
        }catch(AuthenticationException ex){
            handler.sendMessage(new Message(Message.Type.ERROR,msg.getRequestId(),"","SERVER",0,ex.getMessage(),null));
        }catch(ClassCastException ex){
            handler.sendMessage(new Message(Message.Type.ERROR,msg.getRequestId(),"","SERVER",0,"Du lieu dang ky khong hop le",null));
        }finally{
            stateLock.unlock();
        }
    }
    private void processJoin(Message msg,ClientHandler handler){
        stateLock.lock();
        try{
            handler.setUsername(msg.getSender());
            List<Auction> snapshot=auctionService.getAllAuctions();
            handler.sendMessage(new Message(Message.Type.SYNC_STATE,msg.getRequestId(),"","SERVER",0,buildSummaryText(snapshot),snapshot));
            broadcast(new Message(Message.Type.INFO,"","SERVER",0,handler.getUsername()+" vua tham gia san dau gia!"));
        }finally{
            stateLock.unlock();
        }
    }
    private void processCreateAuction(Message msg,ClientHandler handler){
        stateLock.lock();
        try{
            String[] params=(String[])msg.getPayload();
            if(params==null||params.length<9){
                handler.sendMessage(new Message(Message.Type.ERROR,msg.getRequestId(),"","SERVER",0,"Thieu thong tin tao phien dau gia",null));
                return;
            }
            User actor=userDao.findByUsername(params[0]);
            if(actor==null||actor instanceof Bidder){
                handler.sendMessage(new Message(Message.Type.ERROR,msg.getRequestId(),"","SERVER",0,"Chi SELLER hoac ADMIN moi duoc tao phien",null));
                return;
            }
            Seller seller=actor instanceof Seller?(Seller)actor:findDefaultSeller();
            if(seller==null){
                handler.sendMessage(new Message(Message.Type.ERROR,msg.getRequestId(),"","SERVER",0,"Khong tim thay tai khoan SELLER hop le",null));
                return;
            }
            double startPrice=Double.parseDouble(params[4]);
            double increment=Double.parseDouble(params[7]);
            long hours=Long.parseLong(params[8]);
            if(hours<=0){
                throw new IllegalArgumentException("Thoi luong phai lon hon 0 gio");
            }
            Auction created=auctionService.createAuction(seller,params[1],params[2],params[3],startPrice,params[5],params[6],increment,LocalDateTime.now().plusHours(hours));
            persistState();
            List<Auction> snapshot=auctionService.getAllAuctions();
            String info="Phien moi "+created.getAuctionId()+" ("+created.getItem().getName()+") da duoc tao boi "+actor.getUsername();
            handler.sendMessage(new Message(Message.Type.INFO,msg.getRequestId(),created.getAuctionId(),"SERVER",created.getCurrentPrice(),info,created));
            broadcast(new Message(Message.Type.UPDATE,null,created.getAuctionId(),actor.getUsername(),created.getCurrentPrice(),info,snapshot));
        }catch(NumberFormatException ex){
            handler.sendMessage(new Message(Message.Type.ERROR,msg.getRequestId(),"","SERVER",0,"Dinh dang so khong hop le",null));
        }catch(AuthenticationException|IllegalArgumentException ex){
            handler.sendMessage(new Message(Message.Type.ERROR,msg.getRequestId(),"","SERVER",0,ex.getMessage(),null));
        }catch(ClassCastException ex){
            handler.sendMessage(new Message(Message.Type.ERROR,msg.getRequestId(),"","SERVER",0,"Du lieu tao phien khong hop le",null));
        }finally{
            stateLock.unlock();
        }
    }
    private void processBid(Message msg,ClientHandler handler){
        stateLock.lock();
        try{
            User user=userDao.findByUsername(msg.getSender());
            if(user==null){
                handler.sendMessage(new Message(Message.Type.ERROR,msg.getRequestId(),msg.getAuctionId(),"SERVER",0,"Tai khoan khong ton tai tren Server: "+msg.getSender(),null));
                return;
            }
            if(!(user instanceof Bidder bidder)){
                handler.sendMessage(new Message(Message.Type.ERROR,msg.getRequestId(),msg.getAuctionId(),"SERVER",0,"Chi tai khoan BIDDER moi duoc dat gia",null));
                return;
            }
            auctionService.placeBid(msg.getAuctionId(),bidder,msg.getAmount());
            persistState();
            List<Auction> snapshot=auctionService.getAllAuctions();
            String updateText=String.format("Phien %s co gia moi: %,.0f VND (boi %s)",msg.getAuctionId(),msg.getAmount(),bidder.getUsername());
            broadcast(new Message(Message.Type.UPDATE,null,msg.getAuctionId(),bidder.getUsername(),msg.getAmount(),updateText,snapshot));
        }catch(InvalidBidException|AuctionClosedException|AuthenticationException ex){
            handler.sendMessage(new Message(Message.Type.ERROR,msg.getRequestId(),msg.getAuctionId(),"SERVER",0,ex.getMessage(),null));
        }finally{
            stateLock.unlock();
        }
    }
    private void processStartAuction(Message msg,ClientHandler handler){
        stateLock.lock();
        try{
            requireManagerPermission(msg.getSender());
            auctionService.startAuction(msg.getAuctionId());
            persistState();
            List<Auction> snapshot=auctionService.getAllAuctions();
            String text="Phien "+msg.getAuctionId()+" da chuyen sang RUNNING";
            broadcast(new Message(Message.Type.UPDATE,null,msg.getAuctionId(),msg.getSender(),0,text,snapshot));
        }catch(AuthenticationException|AuctionClosedException ex){
            handler.sendMessage(new Message(Message.Type.ERROR,msg.getRequestId(),msg.getAuctionId(),"SERVER",0,ex.getMessage(),null));
        }finally{
            stateLock.unlock();
        }
    }
    private void processFinishAuction(Message msg,ClientHandler handler){
        stateLock.lock();
        try{
            requireManagerPermission(msg.getSender());
            Bidder winner=auctionService.finishAuction(msg.getAuctionId());
            persistState();
            List<Auction> snapshot=auctionService.getAllAuctions();
            String winName=winner!=null?winner.getUsername():"Khong co nguoi dat";
            String text="Phien "+msg.getAuctionId()+" da FINISHED | Nguoi thang: "+winName;
            broadcast(new Message(Message.Type.UPDATE,null,msg.getAuctionId(),msg.getSender(),0,text,snapshot));
        }catch(AuthenticationException|AuctionClosedException ex){
            handler.sendMessage(new Message(Message.Type.ERROR,msg.getRequestId(),msg.getAuctionId(),"SERVER",0,ex.getMessage(),null));
        }finally{
            stateLock.unlock();
        }
    }
    private void processPayAuction(Message msg,ClientHandler handler){
        stateLock.lock();
        try{
            requireManagerPermission(msg.getSender());
            auctionService.markAsPaid(msg.getAuctionId());
            persistState();
            List<Auction> snapshot=auctionService.getAllAuctions();
            String text="Phien "+msg.getAuctionId()+" da xac nhan thanh toan (PAID)";
            broadcast(new Message(Message.Type.UPDATE,null,msg.getAuctionId(),msg.getSender(),0,text,snapshot));
        }catch(AuthenticationException|AuctionClosedException ex){
            handler.sendMessage(new Message(Message.Type.ERROR,msg.getRequestId(),msg.getAuctionId(),"SERVER",0,ex.getMessage(),null));
        }finally{
            stateLock.unlock();
        }
    }
    private void processCancelAuction(Message msg,ClientHandler handler){
        stateLock.lock();
        try{
            requireManagerPermission(msg.getSender());
            auctionService.cancelAuction(msg.getAuctionId());
            persistState();
            List<Auction> snapshot=auctionService.getAllAuctions();
            String text="Phien "+msg.getAuctionId()+" da bi huy (CANCELED)";
            broadcast(new Message(Message.Type.UPDATE,null,msg.getAuctionId(),msg.getSender(),0,text,snapshot));
        }catch(AuthenticationException|AuctionClosedException ex){
            handler.sendMessage(new Message(Message.Type.ERROR,msg.getRequestId(),msg.getAuctionId(),"SERVER",0,ex.getMessage(),null));
        }finally{
            stateLock.unlock();
        }
    }
    private void requireManagerPermission(String username)throws AuthenticationException{
        User user=userDao.findByUsername(username);
        if(user==null||!user.isActive()||user instanceof Bidder){
            throw new AuthenticationException("Chi tai khoan SELLER hoac ADMIN moi duoc quan ly trang thai phien");
        }
    }
    private Seller findDefaultSeller(){
        List<User> all=userDao.findAll();
        for(int i=0;i<all.size();i++){
            User u=all.get(i);
            if(u instanceof Seller seller){
                return seller;
            }
        }
        return null;
    }
    private void persistState(){
        try{
            List<Auction> allAuctions=auctionDao.findAll();
            List<Item> allItems=new ArrayList<>();
            for(int i=0;i<allAuctions.size();i++){
                if(allAuctions.get(i).getItem()!=null){
                    allItems.add(allAuctions.get(i).getItem());
                }
            }
            storage.save(userDao.findAll(),allItems,allAuctions);
        }catch(IOException ex){
            System.err.println("[Server] Loi luu du lieu Serialization: "+ex.getMessage());
        }
    }
    private String buildSummaryText(List<Auction> auctions){
        StringBuilder sb=new StringBuilder("Danh sach phien hien tai:\n");
        for(int i=0;i<auctions.size();i++){
            Auction a=auctions.get(i);
            String leader=a.getHighestBidder()!=null?a.getHighestBidder().getUsername():"Chua co";
            sb.append(String.format(" - %s | %s | Gia: %,.0f VND | Buoc: %,.0f | Dan dau: %s | %s%n",a.getAuctionId(),a.getItem().getName(),a.getCurrentPrice(),a.getBidIncrement(),leader,a.getStatus().name()));
        }
        return sb.toString();
    }
    public void broadcast(Message msg){
        for(int i=0;i<clients.size();i++){
            clients.get(i).sendMessage(msg);
        }
    }
    public void removeClient(ClientHandler client){
        clients.remove(client);
    }
    public static void main(String[] args){
        AuctionServer server=new AuctionServer();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        try{
            server.start();
        }catch(IOException ex){
            System.err.println("[Server] Loi khoi dong: "+ex.getMessage());
        }
    }
}
