import factory.ItemFactory;
import java.time.LocalDateTime;
import manager.AuctionManager;
import model.Auction;
import model.Bidder;
import model.Item;
import model.Seller;
public class Main{
    public static void main(String[] args){
        AuctionManager manager=AuctionManager.getInstance();
        Seller seller=new Seller("U01","nguoiban","1234","seller@gmail.com");
        Bidder bidder1=new Bidder("U02","nguoimua1","1234","b1@gmail.com",50000000);
        Bidder bidder2=new Bidder("U03","nguoimua2","1234","b2@gmail.com",60000000);
        manager.addUser(seller);
        manager.addUser(bidder1);
        manager.addUser(bidder2);
        Item laptop=ItemFactory.createItem("ELECTRONICS","I01","MacBook Pro","May cu 99%",15000000,"U01","Apple","12");
        Item painting=ItemFactory.createItem("ART","I02","Tranh Son Dau","Tranh phong canh",8000000,"U01","To Ngoc Van","1940");
        Item bike=ItemFactory.createItem("VEHICLE","I03","Honda SH","Xe di it",40000000,"U01","Honda","5000.5");
        seller.addItem(laptop);
        seller.addItem(painting);
        seller.addItem(bike);
        manager.addItem(laptop);
        manager.addItem(painting);
        manager.addItem(bike);
        for(int i=0;i<manager.getItems().size();i++){
            Item it=manager.getItems().get(i);
            System.out.println(it.getItemDetails()+" | Phi ship: "+it.calculateShippingFee());
        }
        Auction auction=seller.createAuction(laptop,500000,LocalDateTime.now().plusHours(2));
        auction.startAuction();
        manager.addAuction(auction);
        bidder1.placeBid(auction,16000000);
        bidder2.placeBid(auction,17500000);
        Bidder winner=auction.closeAuction();
        System.out.println("Nguoi thang dau gia: "+winner.getUsername()+" voi gia "+auction.getCurrentPrice());
    }
}
