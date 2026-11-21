package ui;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import service.AppContext;
public class AuctionMvcApp extends Application{
    private static Stage primaryStage;
    @Override
    public void start(Stage stage)throws Exception{
        primaryStage=stage;
        primaryStage.setOnCloseRequest(e->AppContext.getInstance().disconnectNetworkClient());
        AppContext.getInstance();
        showLogin();
    }
    public static URL resolveFxml(String fileName)throws IOException{
        URL url=AuctionMvcApp.class.getResource("/fxml/"+fileName);
        if(url!=null){
            return url;
        }
        Path fallback=Paths.get("src","main","resources","fxml",fileName);
        if(Files.exists(fallback)){
            return fallback.toUri().toURL();
        }
        throw new IOException("Khong tim thay file FXML: "+fileName);
    }
    public static void showLogin()throws IOException{
        FXMLLoader loader=new FXMLLoader(resolveFxml("login.fxml"));
        Parent root=loader.load();
        primaryStage.setTitle("Auction Pro MVC - Dang nhap");
        primaryStage.setScene(new Scene(root,520,480));
        primaryStage.centerOnScreen();
        primaryStage.show();
    }
    public static void showDashboard()throws IOException{
        FXMLLoader loader=new FXMLLoader(resolveFxml("dashboard.fxml"));
        Parent root=loader.load();
        primaryStage.setTitle("Auction Pro MVC - San dau gia truc tuyen");
        primaryStage.setScene(new Scene(root,1060,620));
        primaryStage.centerOnScreen();
        primaryStage.show();
    }
    public static void showCreateAuctionModal()throws IOException{
        FXMLLoader loader=new FXMLLoader(resolveFxml("create_auction.fxml"));
        Parent root=loader.load();
        Stage modal=new Stage();
        modal.initOwner(primaryStage);
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Tao phien dau gia moi");
        modal.setScene(new Scene(root,440,500));
        modal.showAndWait();
    }
    public static void main(String[] args){
        launch(args);
    }
}
