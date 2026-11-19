package model;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
public class Bidder extends User{
    private double balance;
    private List<Bid> bidHistory;
    public Bidder(String userId,String username,String password,String email,double balance){
        super(userId,username,password,email);
        if(balance<0){
            throw new IllegalArgumentException("So du ban dau khong duoc am");
        }
        this.balance=balance;
        this.bidHistory=new ArrayList<>();
    }
    public double getBalance(){
        return balance;
    }
    public List<Bid> getBidHistory(){
        return bidHistory;
    }
    public void deposit(double amount){
        if(amount<=0){
            throw new IllegalArgumentException("So tien nap phai lon hon 0");
        }
        this.balance+=amount;
    }
    public Bid placeBid(Auction auction,double amount){
        if(!isActive()){
            throw new IllegalStateException("Tai khoan da bi khoa");
        }
        if(amount>balance){
            throw new IllegalArgumentException("So du khong du de dat gia");
        }
        Bid bid=new Bid("BID-"+System.currentTimeMillis(),auction.getAuctionId(),this,amount,LocalDateTime.now());
        auction.addBid(bid);
        bidHistory.add(bid);
        return bid;
    }
    @Override
    public String getRole(){
        return "BIDDER";
    }
    @Override
    public void displayDashboard(){
        System.out.println("Bang dieu khien Bidder: "+getUsername()+" | So du: "+balance);
    }
}