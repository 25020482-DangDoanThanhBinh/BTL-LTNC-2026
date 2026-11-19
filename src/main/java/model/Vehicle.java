package model;
public class Vehicle extends Item{
    private String manufacturer;
    private double mileage;
    public Vehicle(String itemId,String name,String description,double startingPrice,String sellerId,String manufacturer,double mileage){
        super(itemId,name,description,startingPrice,sellerId);
        this.manufacturer=manufacturer;
        this.mileage=mileage;
    }
    public String getManufacturer(){
        return manufacturer;
    }
    public double getMileage(){
        return mileage;
    }
    @Override
    public String getCategory(){
        return "VEHICLE";
    }
    @Override
    public String getItemDetails(){
        return "[Phuong tien] "+getName()+" - Hang xe: "+manufacturer+" - Da di: "+mileage+" km";
    }
    @Override
    public double calculateShippingFee(){
        return 500000+getStartingPrice()*0.005;
    }
}