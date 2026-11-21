package storage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import model.Auction;
import model.Item;
import model.User;
public class DataStorage{
    public static final String DEFAULT_FILE="data/auction_data.dat";
    private final Path filePath;
    private final ReentrantLock ioLock;
    public DataStorage(){
        this(Paths.get(DEFAULT_FILE));
    }
    public DataStorage(String filePath){
        this(Paths.get(filePath));
    }
    public DataStorage(Path filePath){
        this.filePath=filePath;
        this.ioLock=new ReentrantLock();
    }
    public void save(List<User> users,List<Item> items,List<Auction> auctions)throws IOException{
        ioLock.lock();
        try{
            Path parent=filePath.getParent();
            if(parent!=null&&!Files.exists(parent)){
                Files.createDirectories(parent);
            }
            AuctionDataSnapshot snapshot=new AuctionDataSnapshot(users,items,auctions);
            Path tempFile=filePath.resolveSibling(filePath.getFileName().toString()+".tmp");
            try(ObjectOutputStream oos=new ObjectOutputStream(Files.newOutputStream(tempFile))){
                oos.writeObject(snapshot);
                oos.flush();
            }
            try{
                Files.move(tempFile,filePath,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
            }catch(IOException ex){
                Files.move(tempFile,filePath,StandardCopyOption.REPLACE_EXISTING);
            }
        }finally{
            ioLock.unlock();
        }
    }
    public AuctionDataSnapshot load()throws IOException,ClassNotFoundException{
        ioLock.lock();
        try{
            if(!Files.exists(filePath)||Files.size(filePath)==0){
                return null;
            }
            try(ObjectInputStream ois=new ObjectInputStream(Files.newInputStream(filePath))){
                Object obj=ois.readObject();
                if(obj instanceof AuctionDataSnapshot snapshot){
                    return snapshot;
                }
                throw new IOException("Du lieu luu tru khong dung dinh dang AuctionDataSnapshot");
            }
        }finally{
            ioLock.unlock();
        }
    }
    public boolean exists(){
        return Files.exists(filePath);
    }
    public Path getFilePath(){
        return filePath;
    }
}
