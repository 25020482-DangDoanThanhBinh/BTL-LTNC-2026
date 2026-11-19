package factory;
import model.Art;
import model.Electronics;
import model.Item;
import model.Vehicle;
public class ItemFactory{
    public static Item createItem(String type,String itemId,String name,String description,double startingPrice,String sellerId,String attr1,String attr2){
        if(type==null){
            throw new IllegalArgumentException("Loai san pham khong duoc null");
        }
        if(type.equalsIgnoreCase("ELECTRONICS")){
            return new Electronics(itemId,name,description,startingPrice,sellerId,attr1,Integer.parseInt(attr2));
        }else if(type.equalsIgnoreCase("ART")){
            return new Art(itemId,name,description,startingPrice,sellerId,attr1,Integer.parseInt(attr2));
        }else if(type.equalsIgnoreCase("VEHICLE")){
            return new Vehicle(itemId,name,description,startingPrice,sellerId,attr1,Double.parseDouble(attr2));
        }
        throw new IllegalArgumentException("Loai san pham khong hop le: "+type);
    }
}