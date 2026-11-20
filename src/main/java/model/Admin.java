package model;
public class Admin extends User{
    public Admin(String userId,String username,String password,String email){
        super(userId,username,password,email);
    }
    public void banUser(User user){
        if(user!=null){
            user.setActive(false);
        }
    }
    public void cancelAuction(Auction auction){
        if(auction!=null){
            auction.setStatus(AuctionStatus.CANCELED);
        }
    }
    @Override
    public String getRole(){
        return "ADMIN";
    }
    @Override
    public void displayDashboard(){
        System.out.println("Bang dieu khien Admin: "+getUsername());
    }
}
