package network;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
public class ClientHandler implements Runnable{
    private final Socket socket;
    private final AuctionServer server;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username="An danh";
    private volatile boolean closed;
    public ClientHandler(Socket socket,AuctionServer server){
        this.socket=socket;
        this.server=server;
        this.closed=false;
    }
    public String getUsername(){
        return username;
    }
    public void setUsername(String username){
        if(username!=null&&!username.trim().isEmpty()){
            this.username=username.trim();
        }
    }
    @Override
    public void run(){
        try{
            out=new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in=new ObjectInputStream(socket.getInputStream());
            Object obj;
            while(!closed&&(obj=in.readObject())!=null){
                if(obj instanceof Message msg){
                    server.handleClientMessage(msg,this);
                }
            }
        }catch(IOException|ClassNotFoundException ex){
            if(!closed){
                System.out.println("[Server] "+username+" da ngat ket noi.");
            }
        }finally{
            close();
        }
    }
    public synchronized void sendMessage(Message msg){
        if(closed||out==null){
            return;
        }
        try{
            out.reset();
            out.writeObject(msg);
            out.flush();
        }catch(IOException ex){
            close();
        }
    }
    public void close(){
        if(closed){
            return;
        }
        closed=true;
        server.removeClient(this);
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
}
