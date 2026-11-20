package model;
import java.io.Serializable;
public abstract class Item implements Serializable{
    private static final long serialVersionUID=1L;
    private String itemId;
    private String name;
    private String description;
    private double startingPrice;
    private String sellerId;
    public Item(String itemId,String name,String description,double startingPrice,String sellerId){
        if(startingPrice<=0){
            throw new IllegalArgumentException("Gia khoi diem phai lon hon 0");
        }
        this.itemId=itemId;
        this.name=name;
        this.description=description;
        this.startingPrice=startingPrice;
        this.sellerId=sellerId;
    }
    public String getItemId(){
        return itemId;
    }
    public String getName(){
        return name;
    }
    public void setName(String name){
        this.name=name;
    }
    public String getDescription(){
        return description;
    }
    public void setDescription(String description){
        this.description=description;
    }
    public double getStartingPrice(){
        return startingPrice;
    }
    public void setStartingPrice(double startingPrice){
        if(startingPrice<=0){
            throw new IllegalArgumentException("Gia khoi diem phai lon hon 0");
        }
        this.startingPrice=startingPrice;
    }
    public String getSellerId(){
        return sellerId;
    }
    public abstract String getCategory();
    public abstract String getItemDetails();
    public abstract double calculateShippingFee();
}
