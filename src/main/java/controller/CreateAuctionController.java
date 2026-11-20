package controller;
import exception.AuthenticationException;
import java.io.IOException;
import java.time.LocalDateTime;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.Seller;
import model.User;
import service.AppContext;
public class CreateAuctionController{
    @FXML
    private ComboBox<String> typeBox;
    @FXML
    private TextField nameField;
    @FXML
    private TextField descField;
    @FXML
    private TextField startPriceField;
    @FXML
    private TextField incrementField;
    @FXML
    private TextField durationHoursField;
    @FXML
    private Label attr1Label;
    @FXML
    private TextField attr1Field;
    @FXML
    private Label attr2Label;
    @FXML
    private TextField attr2Field;
    @FXML
    private Label errorLabel;
    @FXML
    public void initialize(){
        typeBox.setItems(FXCollections.observableArrayList("ELECTRONICS","ART","VEHICLE"));
        typeBox.getSelectionModel().select("ELECTRONICS");
        durationHoursField.setText("2");
        updateAttrLabels("ELECTRONICS");
        typeBox.valueProperty().addListener((obs,oldVal,newVal)->updateAttrLabels(newVal));
    }
    private void updateAttrLabels(String type){
        if("ART".equals(type)){
            attr1Label.setText("Tac gia");
            attr2Label.setText("Nam sang tac (so nguyen)");
        }else if("VEHICLE".equals(type)){
            attr1Label.setText("Hang xe");
            attr2Label.setText("So km da di (so thuc)");
        }else{
            attr1Label.setText("Hang san xuat");
            attr2Label.setText("Bao hanh (so thang)");
        }
    }
    @FXML
    private void handleCreate(){
        errorLabel.setText("");
        try{
            User current=AppContext.getInstance().getCurrentUser();
            double startPrice=Double.parseDouble(startPriceField.getText().replace(",","").trim());
            double increment=Double.parseDouble(incrementField.getText().replace(",","").trim());
            long hours=Long.parseLong(durationHoursField.getText().trim());
            if(hours<=0){
                throw new IllegalArgumentException("Thoi luong phai lon hon 0 gio");
            }
            if(AppContext.getInstance().isOnline()){
                String actorName=current!=null?current.getUsername():"nguoiban";
                AppContext.getInstance().getNetworkClient().createAuction(actorName,typeBox.getValue(),nameField.getText(),descField.getText(),startPrice,attr1Field.getText().trim(),attr2Field.getText().trim(),increment,hours);
            }else{
                Seller seller=current instanceof Seller?(Seller)current:AppContext.getInstance().findDefaultSeller();
                AppContext.getInstance().getAuctionService().createAuction(seller,typeBox.getValue(),nameField.getText(),descField.getText(),startPrice,attr1Field.getText().trim(),attr2Field.getText().trim(),increment,LocalDateTime.now().plusHours(hours));
            }
            closeWindow();
        }catch(NumberFormatException ex){
            errorLabel.setText("Vui long nhap dung dinh dang so cho gia/buoc gia/thuoc tinh");
        }catch(IOException ex){
            errorLabel.setText("Loi gui yeu cau len Server: "+ex.getMessage());
        }catch(AuthenticationException|IllegalArgumentException ex){
            errorLabel.setText(ex.getMessage());
        }
    }
    @FXML
    private void handleCancel(){
        closeWindow();
    }
    private void closeWindow(){
        Stage stage=(Stage)nameField.getScene().getWindow();
        stage.close();
    }
}
