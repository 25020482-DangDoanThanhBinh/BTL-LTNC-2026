package model;
public class Art extends Item{
    private String artist;
    private int yearCreated;
    public Art(String itemId,String name,String description,double startingPrice,String sellerId,String artist,int yearCreated){
        super(itemId,name,description,startingPrice,sellerId);
        this.artist=artist;
        this.yearCreated=yearCreated;
    }
    public String getArtist(){
        return artist;
    }
    public int getYearCreated(){
        return yearCreated;
    }
    @Override
    public String getCategory(){
        return "ART";
    }
    @Override
    public String getItemDetails(){
        return "[Nghe thuat] "+getName()+" - Tac gia: "+artist+" - Nam: "+yearCreated;
    }
    @Override
    public double calculateShippingFee(){
        return 100000+getStartingPrice()*0.02;
    }
}
