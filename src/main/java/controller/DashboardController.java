package controller;
import exception.AuctionClosedException;
import exception.AuthenticationException;
import exception.InvalidBidException;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Auction;
import model.AuctionStatus;
import model.Bidder;
import model.User;
import network.AuctionClient;
import network.AuctionServer;
import network.Message;
import observer.AuctionObserver;
import service.AppContext;
import service.IAuctionService;
import ui.AuctionMvcApp;
public class DashboardController implements AuctionObserver{
    private static final DateTimeFormatter TIME_FMT=DateTimeFormatter.ofPattern("HH:mm:ss");
    @FXML
    private Label userInfoLabel;
    @FXML
    private TableView<Auction> auctionTable;
    @FXML
    private TableColumn<Auction,String> idCol;
    @FXML
    private TableColumn<Auction,String> itemCol;
    @FXML
    private TableColumn<Auction,String> categoryCol;
    @FXML
    private TableColumn<Auction,String> priceCol;
    @FXML
    private TableColumn<Auction,String> bidderCol;
    @FXML
    private TableColumn<Auction,String> statusCol;
    @FXML
    private Label detailTitleLabel;
    @FXML
    private Label itemDetailLabel;
    @FXML
    private Label shippingLabel;
    @FXML
    private Label minBidLabel;
    @FXML
    private TextField bidAmountField;
    @FXML
    private Button placeBidBtn;
    @FXML
    private Button quickBidBtn;
    @FXML
    private Button startBtn;
    @FXML
    private Button finishBtn;
    @FXML
    private Button payBtn;
    @FXML
    private Button cancelBtn;
    @FXML
    private Button createAuctionBtn;
    @FXML
    private HBox manageBar;
    @FXML
    private Label feedbackLabel;
    @FXML
    private TextArea logArea;
    private IAuctionService auctionService;
    private User currentUser;
    private ObservableList<Auction> auctionData;
    private AuctionClient socketClient;
    private boolean isSocketConnected;
    @FXML
    public void initialize(){
        auctionService=AppContext.getInstance().getAuctionService();
        currentUser=AppContext.getInstance().getCurrentUser();
        setupUserHeader();
        setupTable();
        auctionService.addObserver(this);
        connectOrReuseSocket();
        if(!isSocketConnected){
            reloadAuctions();
            if(!auctionData.isEmpty()){
                auctionTable.getSelectionModel().selectFirst();
            }
        }
    }
    private void connectOrReuseSocket(){
        String username=currentUser!=null?currentUser.getUsername():"Guest";
        AuctionClient existing=AppContext.getInstance().getNetworkClient();
        if(existing!=null&&existing.isConnected()){
            socketClient=existing;
            socketClient.setOnMessageReceived(this::handleServerMessage);
            try{
                socketClient.join();
                isSocketConnected=true;
                logArea.appendText("[Socket] Da ket noi toi AuctionServer (Cong "+AuctionServer.PORT+")\n");
                return;
            }catch(IOException ex){
                AppContext.getInstance().disconnectNetworkClient();
            }
        }
        socketClient=new AuctionClient(username);
        socketClient.setOnMessageReceived(this::handleServerMessage);
        try{
            socketClient.connect("localhost",AuctionServer.PORT,true);
            AppContext.getInstance().setNetworkClient(socketClient);
            isSocketConnected=true;
            logArea.appendText("[Socket] Da ket noi toi AuctionServer (Cong "+AuctionServer.PORT+")\n");
        }catch(IOException ex){
            socketClient.disconnect();
            socketClient=null;
            isSocketConnected=false;
            logArea.appendText("[Offline] Chua bat AuctionServer, dang chay du lieu cuc bo.\n");
        }
    }
    private void handleServerMessage(Message msg){
        Platform.runLater(()->{
            String time=LocalTime.now().format(TIME_FMT);
            switch(msg.getType()){
                case SYNC_STATE->{
                    applyAuctionListPayload(msg.getPayload());
                    logArea.appendText(String.format("[%s] [Sync] Da dong bo danh sach phien tu Server%n",time));
                }
                case UPDATE->{
                    applyAuctionListPayload(msg.getPayload());
                    logArea.appendText(String.format("[%s] [Realtime] %s%n",time,msg.getContent()));
                    if(currentUser!=null&&currentUser.getUsername().equals(msg.getSender())){
                        showSuccess(msg.getContent());
                    }
                }
                case ERROR->showError(msg.getContent());
                case INFO->logArea.appendText(String.format("[%s] [Server] %s%n",time,msg.getContent()));
                default->{
                }
            }
        });
    }
    private void applyAuctionListPayload(Object payload){
        if(!(payload instanceof List<?> rawList)){
            return;
        }
        List<Auction> synced=new ArrayList<>();
        for(int i=0;i<rawList.size();i++){
            Object item=rawList.get(i);
            if(item instanceof Auction a){
                synced.add(a);
            }
        }
        Auction prev=auctionTable.getSelectionModel().getSelectedItem();
        String prevId=prev!=null?prev.getAuctionId():null;
        auctionData=FXCollections.observableArrayList(synced);
        auctionTable.setItems(auctionData);
        if(prevId!=null){
            for(int i=0;i<auctionData.size();i++){
                if(auctionData.get(i).getAuctionId().equals(prevId)){
                    auctionTable.getSelectionModel().select(auctionData.get(i));
                    updateSelectedDetails(auctionData.get(i));
                    return;
                }
            }
        }
        if(!auctionData.isEmpty()){
            auctionTable.getSelectionModel().selectFirst();
        }
    }
    private void setupUserHeader(){
        if(currentUser instanceof Bidder b){
            userInfoLabel.setText(b.getUsername()+" ("+b.getRole()+") | So du: "+String.format("%,.0f VND",b.getBalance()));
            manageBar.setVisible(false);
            manageBar.setManaged(false);
            createAuctionBtn.setVisible(false);
            createAuctionBtn.setManaged(false);
        }else if(currentUser!=null){
            userInfoLabel.setText(currentUser.getUsername()+" ("+currentUser.getRole()+")");
            bidAmountField.setDisable(true);
            placeBidBtn.setDisable(true);
            quickBidBtn.setDisable(true);
        }
    }
    private void setupTable(){
        auctionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        idCol.setCellValueFactory(c->new SimpleStringProperty(c.getValue().getAuctionId()));
        itemCol.setCellValueFactory(c->new SimpleStringProperty(c.getValue().getItem().getName()));
        categoryCol.setCellValueFactory(c->new SimpleStringProperty(c.getValue().getItem().getCategory()));
        priceCol.setCellValueFactory(c->new SimpleStringProperty(String.format("%,.0f VND",c.getValue().getCurrentPrice())));
        bidderCol.setCellValueFactory(c->{
            Bidder hb=c.getValue().getHighestBidder();
            return new SimpleStringProperty(hb!=null?hb.getUsername():"Chua co");
        });
        statusCol.setCellValueFactory(c->new SimpleStringProperty(c.getValue().getStatus().name()));
        statusCol.setCellFactory(col->new TableCell<>(){
            @Override
            protected void updateItem(String status,boolean empty){
                super.updateItem(status,empty);
                if(empty||status==null){
                    setText(null);
                    setStyle("");
                }else{
                    setText(status);
                    if(status.equals(AuctionStatus.RUNNING.name())){
                        setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold;");
                    }else if(status.equals(AuctionStatus.OPEN.name())){
                        setStyle("-fx-text-fill: #1d4ed8; -fx-font-weight: bold;");
                    }else if(status.equals(AuctionStatus.PAID.name())){
                        setStyle("-fx-text-fill: #7e22ce; -fx-font-weight: bold;");
                    }else if(status.equals(AuctionStatus.CANCELED.name())){
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    }else{
                        setStyle("-fx-text-fill: #475569; -fx-font-weight: bold;");
                    }
                }
            }
        });
        auctionTable.getSelectionModel().selectedItemProperty().addListener((obs,oldVal,selected)->updateSelectedDetails(selected));
        auctionTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Auction selected = auctionTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openItemDetailModal(selected);
                }
            }
        });
    }
    private void reloadAuctions(){
        Auction prev=auctionTable.getSelectionModel().getSelectedItem();
        auctionData=FXCollections.observableArrayList(auctionService.getAllAuctions());
        auctionTable.setItems(auctionData);
        if(prev!=null){
            for(int i=0;i<auctionData.size();i++){
                if(auctionData.get(i).getAuctionId().equals(prev.getAuctionId())){
                    auctionTable.getSelectionModel().select(auctionData.get(i));
                    updateSelectedDetails(auctionData.get(i));
                    break;
                }
            }
        }
    }
    private void updateSelectedDetails(Auction selected){
        if(selected==null){
            detailTitleLabel.setText("Chua chon phien dau gia");
            itemDetailLabel.setText("");
            shippingLabel.setText("");
            minBidLabel.setText("");
            return;
        }
        double minNext=selected.getCurrentPrice()+selected.getBidIncrement();
        detailTitleLabel.setText(selected.getAuctionId()+" - "+selected.getItem().getName());
        itemDetailLabel.setText(selected.getItem().getItemDetails());
        shippingLabel.setText("Phi van chuyen du kien: "+String.format("%,.0f VND",selected.getItem().calculateShippingFee()));
        minBidLabel.setText("Gia toi thieu tiep theo: "+String.format("%,.0f VND",minNext));
        bidAmountField.setText(String.format("%.0f",minNext));
    }
    @FXML
    private void handlePlaceBid(){
        Auction selected=auctionTable.getSelectionModel().getSelectedItem();
        if(selected==null){
            showError("Vui long chon mot phien dau gia");
            return;
        }
        if(!(currentUser instanceof Bidder)){
            showError("Chi tai khoan BIDDER moi duoc dat gia");
            return;
        }
        try{
            double amount=Double.parseDouble(bidAmountField.getText().replace(",","").trim());
            if(isSocketConnected&&socketClient!=null&&socketClient.isConnected()){
                socketClient.placeBid(selected.getAuctionId(),amount);
            }else{
                auctionService.placeBid(selected.getAuctionId(),(Bidder)currentUser,amount);
                showSuccess("Dat gia thanh cong: "+String.format("%,.0f VND",amount));
                updateSelectedDetails(selected);
            }
        }catch(NumberFormatException ex){
            showError("Vui long nhap so tien hop le");
        }catch(IOException ex){
            showError("Loi gui lenh len Server: "+ex.getMessage());
        }catch(InvalidBidException|AuctionClosedException|AuthenticationException ex){
            showError(ex.getMessage());
        }
    }
    @FXML
    private void handleQuickBid(){
        Auction selected=auctionTable.getSelectionModel().getSelectedItem();
        if(selected==null){
            showError("Vui long chon mot phien dau gia");
            return;
        }
        if(!(currentUser instanceof Bidder)){
            showError("Chi tai khoan BIDDER moi duoc dat gia");
            return;
        }
        double nextAmount=selected.getCurrentPrice()+selected.getBidIncrement();
        try{
            if(isSocketConnected&&socketClient!=null&&socketClient.isConnected()){
                socketClient.placeBid(selected.getAuctionId(),nextAmount);
            }else{
                auctionService.placeBid(selected.getAuctionId(),(Bidder)currentUser,nextAmount);
                showSuccess("Dat nhanh thanh cong: "+String.format("%,.0f VND",nextAmount));
                updateSelectedDetails(selected);
            }
        }catch(IOException ex){
            showError("Loi gui lenh len Server: "+ex.getMessage());
        }catch(InvalidBidException|AuctionClosedException|AuthenticationException ex){
            showError(ex.getMessage());
        }
    }
    @FXML
    private void handleStartAuction(){
        Auction selected=auctionTable.getSelectionModel().getSelectedItem();
        if(selected==null){
            showError("Chon phien can bat dau");
            return;
        }
        try{
            if(isSocketConnected&&socketClient!=null&&socketClient.isConnected()){
                socketClient.startAuction(selected.getAuctionId());
            }else{
                auctionService.startAuction(selected.getAuctionId());
                showSuccess("Da mo phien "+selected.getAuctionId());
            }
        }catch(IOException|AuctionClosedException ex){
            showError(ex.getMessage());
        }
    }
    @FXML
    private void handleFinishAuction(){
        Auction selected=auctionTable.getSelectionModel().getSelectedItem();
        if(selected==null){
            showError("Chon phien can ket thuc");
            return;
        }
        try{
            if(isSocketConnected&&socketClient!=null&&socketClient.isConnected()){
                socketClient.finishAuction(selected.getAuctionId());
            }else{
                Bidder winner=auctionService.finishAuction(selected.getAuctionId());
                String winName=winner!=null?winner.getUsername():"Khong co nguoi dat";
                showSuccess("Ket thuc phien "+selected.getAuctionId()+" | Nguoi thang: "+winName);
            }
        }catch(IOException|AuctionClosedException ex){
            showError(ex.getMessage());
        }
    }
    @FXML
    private void handleMarkPaid(){
        Auction selected=auctionTable.getSelectionModel().getSelectedItem();
        if(selected==null){
            showError("Chon phien can xac nhan thanh toan");
            return;
        }
        try{
            if(isSocketConnected&&socketClient!=null&&socketClient.isConnected()){
                socketClient.payAuction(selected.getAuctionId());
            }else{
                auctionService.markAsPaid(selected.getAuctionId());
                showSuccess("Phien "+selected.getAuctionId()+" da chuyen sang PAID");
            }
        }catch(IOException|AuctionClosedException ex){
            showError(ex.getMessage());
        }
    }
    @FXML
    private void handleCancelAuction(){
        Auction selected=auctionTable.getSelectionModel().getSelectedItem();
        if(selected==null){
            showError("Chon phien can huy");
            return;
        }
        try{
            if(isSocketConnected&&socketClient!=null&&socketClient.isConnected()){
                socketClient.cancelAuction(selected.getAuctionId());
            }else{
                auctionService.cancelAuction(selected.getAuctionId());
                showSuccess("Da huy phien "+selected.getAuctionId());
            }
        }catch(IOException|AuctionClosedException ex){
            showError(ex.getMessage());
        }
    }
    @FXML
    private void handleOpenCreateAuction(){
        try{
            AuctionMvcApp.showCreateAuctionModal();
            if(!isSocketConnected){
                reloadAuctions();
            }
        }catch(IOException ex){
            showError("Khong mo duoc form tao phien: "+ex.getMessage());
        }
    }
    @FXML
    private void handleLogout(){
        auctionService.removeObserver(this);
        if(socketClient!=null){
            socketClient.setOnMessageReceived(null);
            socketClient=null;
        }
        isSocketConnected=false;
        AppContext.getInstance().disconnectNetworkClient();
        AppContext.getInstance().setCurrentUser(null);
        try{
            AuctionMvcApp.showLogin();
        }catch(IOException ex){
            showError("Loi dang xuat: "+ex.getMessage());
        }
    }
    private void showSuccess(String msg){
        feedbackLabel.setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold;");
        feedbackLabel.setText(msg);
    }
    private void showError(String msg){
        feedbackLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
        feedbackLabel.setText(msg);
    }
    @Override
    public void onNewBid(String auctionId,String bidderName,double newAmount){
        String msg=String.format("[%s] %s dat %,.0f VND cho %s",LocalTime.now().format(TIME_FMT),bidderName,newAmount,auctionId);
        Platform.runLater(()->{
            logArea.appendText(msg+"\n");
            auctionTable.refresh();
            Auction selected=auctionTable.getSelectionModel().getSelectedItem();
            if(selected!=null&&selected.getAuctionId().equals(auctionId)){
                updateSelectedDetails(selected);
            }
        });
    }
    @Override
    public void onStatusChanged(String auctionId,String newStatus){
        String msg=String.format("[%s] Phien %s -> %s",LocalTime.now().format(TIME_FMT),auctionId,newStatus);
        Platform.runLater(()->{
            logArea.appendText(msg+"\n");
            auctionTable.refresh();
        });
    }
    private void openItemDetailModal(Auction auction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/item_detail.fxml"));
            Parent root = loader.load();
            ItemDetailController controller = loader.getController();
            controller.setAuction(auction);

            Stage modal = new Stage();
            modal.setTitle("Chi tiet san pham - " + auction.getAuctionId());
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setScene(new Scene(root));
            modal.showAndWait();
        } catch (IOException e) {
            showError("Khong mo duoc chi tiet san pham: " + e.getMessage());
        }
    }
}
