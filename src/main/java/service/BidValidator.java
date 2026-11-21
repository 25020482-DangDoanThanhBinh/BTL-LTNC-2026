package service;
import exception.AuctionClosedException;
import exception.AuthenticationException;
import exception.InvalidBidException;
import model.Auction;
import model.AuctionStatus;
import model.Bidder;
public class BidValidator{
    public void validateBid(Auction auction,Bidder bidder,double amount)throws InvalidBidException,AuctionClosedException,AuthenticationException{
        if(bidder==null){
            throw new AuthenticationException("Nguoi dat gia khong hop le");
        }
        if(!bidder.isActive()){
            throw new AuthenticationException("Tai khoan da bi khoa");
        }
        if(auction==null){
            throw new AuctionClosedException("Khong tim thay phien dau gia");
        }
        if(auction.getStatus()!=AuctionStatus.RUNNING){
            throw new AuctionClosedException("Phien dau gia khong o trang thai RUNNING (hien tai: "+auction.getStatus()+")");
        }
        if(auction.isExpired()){
            throw new AuctionClosedException("Phien dau gia da het thoi gian");
        }
        if(auction.getSeller()!=null&&auction.getSeller().getUserId().equals(bidder.getUserId())){
            throw new InvalidBidException("Nguoi ban khong the tu dat gia san pham cua minh");
        }
        if(Double.isNaN(amount)||Double.isInfinite(amount)||amount<=0){
            throw new InvalidBidException("So tien dat gia khong hop le");
        }
        double minNext=auction.getCurrentPrice()+auction.getBidIncrement();
        if(amount<minNext){
            throw new InvalidBidException("Gia dat toi thieu phai la "+String.format("%.0f",minNext));
        }
        if(amount>bidder.getBalance()){
            throw new InvalidBidException("So du khong du de dat gia");
        }
    }
}
