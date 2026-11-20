package factory;
import model.Art;
import model.Electronics;
import model.Item;
import model.Vehicle;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class ItemFactoryTest{
    @Test
    void testCreateValidItemsAndPolymorphicBehavior(){
        Item e=ItemFactory.createItem("ELECTRONICS","I1","Laptop","Dep",10000000,"S1","Dell","12");
        Item a=ItemFactory.createItem("ART","I2","Tranh","Co",5000000,"S1","Hoa Si","1980");
        Item v=ItemFactory.createItem("VEHICLE","I3","Xe May","Moi",40000000,"S1","Honda","1200.5");
        assertInstanceOf(Electronics.class,e);
        assertInstanceOf(Art.class,a);
        assertInstanceOf(Vehicle.class,v);
        assertEquals("ELECTRONICS",e.getCategory());
        assertEquals("ART",a.getCategory());
        assertEquals("VEHICLE",v.getCategory());
        assertTrue(e.calculateShippingFee()>50000);
        assertTrue(a.calculateShippingFee()>100000);
        assertTrue(v.calculateShippingFee()>500000);
    }
    @Test
    void testCreateInvalidOrNullTypeThrowsIllegalArgumentException(){
        assertThrows(IllegalArgumentException.class,()->ItemFactory.createItem(null,"I4","A","B",1000,"S1","x","1"));
        assertThrows(IllegalArgumentException.class,()->ItemFactory.createItem("FOOD","I5","A","B",1000,"S1","x","1"));
    }
}
