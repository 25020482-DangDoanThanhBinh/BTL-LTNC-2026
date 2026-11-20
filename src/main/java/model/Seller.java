package model;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
public class Seller extends User{
    private List<Item> items;
    private List<Auction> createdAuctions;
    public Seller(String userId,String username,String password,String email){
        super(userId,username,password,email);
        this.items=new ArrayList<>();
        this.createdAuctions=new ArrayList<>();
    }
    public List<Item> getItems(){
        return items;
    }
    public List<Auction> getCreatedAuctions(){
        return createdAuctions;
    }
    public void addItem(Item item){
        if(item==null){
            throw new IllegalArgumentException("San pham khong duoc null");
        }
        items.add(item);
    }
    public Auction createAuction(Item item,double step,LocalDateTime endTime){
        if(!isActive()){
            throw new IllegalStateException("Tai khoan da bi khoa");
        }
        Auction auction=new Auction("AUC-"+System.currentTimeMillis(),item,this,step,LocalDateTime.now(),endTime);
        createdAuctions.add(auction);
        return auction;
    }
    @Override
    public String getRole(){
        return "SELLER";
    }
    @Override
    public void displayDashboard(){
        System.out.println("Bang dieu khien Seller: "+getUsername()+" | So san pham: "+items.size());
    }
}
