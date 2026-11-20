package storage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import model.Auction;
import model.Item;
import model.User;
public class AuctionDataSnapshot implements Serializable{
    private static final long serialVersionUID=1L;
    private final List<User> users;
    private final List<Item> items;
    private final List<Auction> auctions;
    public AuctionDataSnapshot(List<User> users,List<Item> items,List<Auction> auctions){
        this.users=users!=null?new ArrayList<>(users):new ArrayList<>();
        this.items=items!=null?new ArrayList<>(items):new ArrayList<>();
        this.auctions=auctions!=null?new ArrayList<>(auctions):new ArrayList<>();
    }
    public List<User> getUsers(){
        return new ArrayList<>(users);
    }
    public List<Item> getItems(){
        return new ArrayList<>(items);
    }
    public List<Auction> getAuctions(){
        return new ArrayList<>(auctions);
    }
}
