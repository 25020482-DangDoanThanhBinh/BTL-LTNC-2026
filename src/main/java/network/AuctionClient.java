package network;
import exception.AuthenticationException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import model.User;
public class AuctionClient{
    private static final int CONNECT_TIMEOUT_MS=1500;
    private static final long REQUEST_TIMEOUT_MS=5000;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;
    private volatile boolean connected;
    private Consumer<Message> onMessageReceived;
    private final ConcurrentHashMap<String,CompletableFuture<Message>> pendingRequests;
    public AuctionClient(String username){
        this.username=username;
        this.connected=false;
        this.pendingRequests=new ConcurrentHashMap<>();
    }
    public void setUsername(String username){
        this.username=username;
    }
    public String getUsername(){
        return username;
    }
    public boolean isConnected(){
        return connected&&socket!=null&&!socket.isClosed();
    }
    public void setOnMessageReceived(Consumer<Message> callback){
        this.onMessageReceived=callback;
    }
    public void connect(String host,int port)throws IOException{
        connect(host,port,true);
    }
    public void connect(String host,int port,boolean autoJoin)throws IOException{
        socket=new Socket();
        socket.connect(new InetSocketAddress(host,port),CONNECT_TIMEOUT_MS);
        out=new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in=new ObjectInputStream(socket.getInputStream());
        connected=true;
        Thread listener=new Thread(this::listenLoop);
        listener.setDaemon(true);
        listener.start();
        if(autoJoin){
            join();
        }
    }
    private void listenLoop(){
        try{
            Object obj;
            while(connected&&(obj=in.readObject())!=null){
                if(obj instanceof Message msg){
                    if(msg.getRequestId()!=null){
                        CompletableFuture<Message> future=pendingRequests.remove(msg.getRequestId());
                        if(future!=null){
                            future.complete(msg);
                            continue;
                        }
                    }
                    Consumer<Message> callback=onMessageReceived;
                    if(callback!=null){
                        callback.accept(msg);
                    }
                }
            }
        }catch(IOException|ClassNotFoundException ex){
            if(connected){
                System.out.println("[Client] Da mat ket noi voi Server.");
            }
        }finally{
            connected=false;
            failAllPending(new IOException("Mat ket noi toi Server"));
        }
    }
    private void failAllPending(Exception cause){
        for(CompletableFuture<Message> f:pendingRequests.values()){
            f.completeExceptionally(cause);
        }
        pendingRequests.clear();
    }
    private Message sendAndWait(Message request)throws IOException{
        CompletableFuture<Message> future=new CompletableFuture<>();
        pendingRequests.put(request.getRequestId(),future);
        try{
            send(request);
            return future.get(REQUEST_TIMEOUT_MS,TimeUnit.MILLISECONDS);
        }catch(InterruptedException ex){
            Thread.currentThread().interrupt();
            throw new IOException("Bi gian doan khi cho phan hoi Server",ex);
        }catch(TimeoutException ex){
            throw new IOException("Het thoi gian cho phan hoi tu Server",ex);
        }catch(ExecutionException ex){
            throw new IOException("Loi nhan phan hoi tu Server: "+ex.getCause().getMessage(),ex);
        }finally{
            pendingRequests.remove(request.getRequestId());
        }
    }
    public User login(String user,String password)throws IOException,AuthenticationException{
        String reqId=UUID.randomUUID().toString();
        Message req=new Message(Message.Type.LOGIN,reqId,"",user,0,"",new String[]{user,password});
        Message res=sendAndWait(req);
        Object payload=res.getPayload();
        if(res.getType()==Message.Type.LOGIN_RESULT&&payload instanceof User logged){
            this.username=logged.getUsername();
            return logged;
        }
        throw new AuthenticationException(res.getContent());
    }
    public User register(String user,String password,String email,String role,double initialBalance)throws IOException,AuthenticationException{
        String reqId=UUID.randomUUID().toString();
        String[] payload=new String[]{user,password,email,role,String.valueOf(initialBalance)};
        Message req=new Message(Message.Type.REGISTER,reqId,"",user,initialBalance,"",payload);
        Message res=sendAndWait(req);
        Object resPayload=res.getPayload();
        if(res.getType()==Message.Type.REGISTER_RESULT&&resPayload instanceof User created){
            return created;
        }
        throw new AuthenticationException(res.getContent());
    }
    public void createAuction(String sellerUsername,String itemType,String name,String desc,double startPrice,String attr1,String attr2,double increment,long durationHours)throws IOException{
        String reqId=UUID.randomUUID().toString();
        String[] payload=new String[]{sellerUsername,itemType,name,desc,String.valueOf(startPrice),attr1,attr2,String.valueOf(increment),String.valueOf(durationHours)};
        Message req=new Message(Message.Type.CREATE_AUCTION,reqId,"",sellerUsername,startPrice,"",payload);
        Message res=sendAndWait(req);
        if(res.getType()==Message.Type.ERROR){
            throw new IllegalArgumentException(res.getContent());
        }
    }
    public void join()throws IOException{
        send(new Message(Message.Type.JOIN,"",username,0,"Joined"));
    }
    public void placeBid(String auctionId,double amount)throws IOException{
        send(new Message(Message.Type.BID,auctionId,username,amount,""));
    }
    public void startAuction(String auctionId)throws IOException{
        send(new Message(Message.Type.START_AUCTION,auctionId,username,0,""));
    }
    public void finishAuction(String auctionId)throws IOException{
        send(new Message(Message.Type.FINISH_AUCTION,auctionId,username,0,""));
    }
    public void payAuction(String auctionId)throws IOException{
        send(new Message(Message.Type.PAY_AUCTION,auctionId,username,0,""));
    }
    public void cancelAuction(String auctionId)throws IOException{
        send(new Message(Message.Type.CANCEL_AUCTION,auctionId,username,0,""));
    }
    private synchronized void send(Message msg)throws IOException{
        if(!isConnected()){
            throw new IOException("Chua ket noi toi Server");
        }
        out.reset();
        out.writeObject(msg);
        out.flush();
    }
    public void disconnect(){
        connected=false;
        failAllPending(new IOException("Client da ngat ket noi"));
        try{
            if(in!=null){
                in.close();
            }
        }catch(IOException ignored){
        }
        try{
            if(out!=null){
                out.close();
            }
        }catch(IOException ignored){
        }
        try{
            if(socket!=null&&!socket.isClosed()){
                socket.close();
            }
        }catch(IOException ignored){
        }
    }
    public static void main(String[] args){
        Scanner sc=new Scanner(System.in);
        System.out.print("Nhap ten dang nhap (vd: nguoimua1 hoac nguoimua2): ");
        String name=sc.nextLine().trim();
        System.out.print("Nhap mat khau (mac dinh: 1234): ");
        String pass=sc.nextLine().trim();
        AuctionClient client=new AuctionClient(name);
        client.setOnMessageReceived(msg->{
            System.out.println("\n>>> ["+msg.getType()+"] "+msg.getContent());
            System.out.print("Nhap lenh (<MaPhien> <SoTien>, vd: AUC-101 31000000): ");
        });
        try{
            client.connect("localhost",AuctionServer.PORT,false);
            client.login(name,pass);
            client.join();
            System.out.println("Ket noi va dang nhap thanh cong! Nhap 'exit' de thoat.");
            while(true){
                String line=sc.nextLine().trim();
                if(line.equalsIgnoreCase("exit")){
                    break;
                }
                String[] parts=line.split("\\s+");
                if(parts.length==2){
                    try{
                        String auctionId=parts[0].toUpperCase();
                        double amount=Double.parseDouble(parts[1]);
                        client.placeBid(auctionId,amount);
                    }catch(NumberFormatException ex){
                        System.out.println("So tien khong hop le!");
                    }
                }else{
                    System.out.println("Sai cu phap! Hay nhap: AUC-101 31000000");
                }
            }
        }catch(AuthenticationException ex){
            System.err.println("Dang nhap that bai: "+ex.getMessage());
        }catch(IOException ex){
            System.err.println("Khong the ket noi Server (hay bat AuctionServer truoc): "+ex.getMessage());
        }finally{
            client.disconnect();
            sc.close();
        }
    }
}
