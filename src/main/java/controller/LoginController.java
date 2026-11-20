package controller;
import exception.AuthenticationException;
import java.io.IOException;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import model.User;
import service.AppContext;
import service.IAuthService;
import ui.AuctionMvcApp;
public class LoginController{
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;
    @FXML
    private TextField regUsernameField;
    @FXML
    private PasswordField regPasswordField;
    @FXML
    private TextField regEmailField;
    @FXML
    private ComboBox<String> regRoleBox;
    @FXML
    private TextField regBalanceField;
    @FXML
    private Label regMessageLabel;
    private IAuthService authService;
    @FXML
    public void initialize(){
        authService=AppContext.getInstance().getAuthService();
        regRoleBox.setItems(FXCollections.observableArrayList("BIDDER","SELLER"));
        regRoleBox.getSelectionModel().select("BIDDER");
        regBalanceField.setText("50000000");
        regRoleBox.valueProperty().addListener((obs,oldVal,newVal)->{
            boolean isSeller="SELLER".equals(newVal);
            regBalanceField.setDisable(isSeller);
            if(isSeller){
                regBalanceField.setText("0");
            }else if(regBalanceField.getText().trim().equals("0")){
                regBalanceField.setText("50000000");
            }
        });
    }
    @FXML
    private void handleLogin(){
        errorLabel.setText("");
        try{
            User user=authService.login(usernameField.getText(),passwordField.getText());
            AppContext.getInstance().setCurrentUser(user);
            AuctionMvcApp.showDashboard();
        }catch(AuthenticationException ex){
            errorLabel.setText(ex.getMessage());
        }catch(IOException ex){
            errorLabel.setText("Loi mo man hinh chinh: "+ex.getMessage());
        }
    }
    @FXML
    private void handleRegister(){
        regMessageLabel.setText("");
        try{
            String role=regRoleBox.getValue();
            double balance=0;
            if("BIDDER".equals(role)){
                balance=Double.parseDouble(regBalanceField.getText().replace(",","").trim());
            }
            User created=authService.register(regUsernameField.getText(),regPasswordField.getText(),regEmailField.getText(),role,balance);
            regMessageLabel.setStyle("-fx-text-fill: #15803d; -fx-font-size: 12px;");
            regMessageLabel.setText("Dang ky thanh cong: "+created.getUsername()+" ("+created.getRole()+")");
            usernameField.setText(created.getUsername());
            passwordField.clear();
        }catch(NumberFormatException ex){
            regMessageLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
            regMessageLabel.setText("So du ban dau phai la so hop le");
        }catch(AuthenticationException ex){
            regMessageLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
            regMessageLabel.setText(ex.getMessage());
        }
    }
}
