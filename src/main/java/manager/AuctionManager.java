package manager;
import java.util.ArrayList;
import java.util.List;
import model.Auction;
import model.Item;
import model.User;
public class AuctionManager{
    private static volatile AuctionManager instance;
    private List<User> users;
    private List<Item> items;
    private List<Auction> auctions;
    private AuctionManager(){
        users=new ArrayList<>();
        items=new ArrayList<>();
        auctions=new ArrayList<>();
    }
    public static AuctionManager getInstance(){
        if(instance==null){
            synchronized(AuctionManager.class){
                if(instance==null){
                    instance=new AuctionManager();
                }
            }
        }
        return instance;
    }
    public void addUser(User user){
        users.add(user);
    }
    public void addItem(Item item){
        items.add(item);
    }
    public void addAuction(Auction auction){
        auctions.add(auction);
    }
    public List<User> getUsers(){
        return users;
    }
    public List<Item> getItems(){
        return items;
    }
    public List<Auction> getAuctions(){
        return auctions;
    }
    public Auction findAuctionById(String auctionId){
        for(int i=0;i<auctions.size();i++){
            if(auctions.get(i).getAuctionId().equals(auctionId)){
                return auctions.get(i);
            }
        }
        return null;
    }
}
