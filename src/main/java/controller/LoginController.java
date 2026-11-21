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
import network.AuctionClient;
import network.AuctionServer;
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
        String username=usernameField.getText();
        String password=passwordField.getText();
        AppContext.getInstance().disconnectNetworkClient();
        AuctionClient client=new AuctionClient(username);
        boolean connectedToServer=false;
        try{
            client.connect("localhost",AuctionServer.PORT,false);
            connectedToServer=true;
            User user=client.login(username,password);
            AppContext.getInstance().setNetworkClient(client);
            AppContext.getInstance().setCurrentUser(user);
            AuctionMvcApp.showDashboard();
        }catch(AuthenticationException ex){
            client.disconnect();
            errorLabel.setText(ex.getMessage());
        }catch(IOException ex){
            client.disconnect();
            if(connectedToServer){
                errorLabel.setText("Loi giao tiep Server: "+ex.getMessage());
                return;
            }
            try{
                User localUser=authService.login(username,password);
                AppContext.getInstance().setCurrentUser(localUser);
                AuctionMvcApp.showDashboard();
            }catch(AuthenticationException authEx){
                errorLabel.setText(authEx.getMessage());
            }catch(IOException uiEx){
                errorLabel.setText("Loi mo man hinh chinh: "+uiEx.getMessage());
            }
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
            User created=registerOnlineOrLocal(regUsernameField.getText(),regPasswordField.getText(),regEmailField.getText(),role,balance);
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
    private User registerOnlineOrLocal(String username,String password,String email,String role,double balance)throws AuthenticationException{
        AuctionClient tempClient=new AuctionClient(username);
        try{
            tempClient.connect("localhost",AuctionServer.PORT,false);
            return tempClient.register(username,password,email,role,balance);
        }catch(IOException ex){
            return authService.register(username,password,email,role,balance);
        }finally{
            tempClient.disconnect();
        }
    }
}
