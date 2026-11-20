package model;
public class Electronics extends Item{
    private String brand;
    private int warrantyMonths;
    public Electronics(String itemId,String name,String description,double startingPrice,String sellerId,String brand,int warrantyMonths){
        super(itemId,name,description,startingPrice,sellerId);
        this.brand=brand;
        this.warrantyMonths=warrantyMonths;
    }
    public String getBrand(){
        return brand;
    }
    public int getWarrantyMonths(){
        return warrantyMonths;
    }
    @Override
    public String getCategory(){
        return "ELECTRONICS";
    }
    @Override
    public String getItemDetails(){
        return "[Do dien tu] "+getName()+" - Hang: "+brand+" - Bao hanh: "+warrantyMonths+" thang";
    }
    @Override
    public double calculateShippingFee(){
        return 50000+getStartingPrice()*0.01;
    }
}
