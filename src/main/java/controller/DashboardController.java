package controller;

import exception.AuctionClosedException;
import exception.AuthenticationException;
import exception.InvalidBidException;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
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

public class DashboardController implements AuctionObserver {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML private Label userInfoLabel;
    @FXML private TableView<Auction> auctionTable;
    @FXML private TableColumn<Auction, String> idCol;
    @FXML private TableColumn<Auction, String> itemCol;
    @FXML private TableColumn<Auction, String> categoryCol;
    @FXML private TableColumn<Auction, String> priceCol;
    @FXML private TableColumn<Auction, String> bidderCol;
    @FXML private TableColumn<Auction, String> statusCol;
    @FXML private Label detailTitleLabel;
    @FXML private Label itemDetailLabel;
    @FXML private Label shippingLabel;
    @FXML private Label minBidLabel;
    @FXML private TextField bidAmountField;
    @FXML private Button placeBidBtn;
    @FXML private Button quickBidBtn;
    @FXML private Button startBtn;
    @FXML private Button finishBtn;
    @FXML private Button payBtn;
    @FXML private Button cancelBtn;
    @FXML private Button createAuctionBtn;
    @FXML private HBox manageBar;
    @FXML private Label feedbackLabel;
    @FXML private TextArea logArea;

    private IAuctionService auctionService;
    private User currentUser;
    private ObservableList<Auction> auctionData;

    // Kết nối Socket Tuần 9 để đồng bộ Realtime giữa nhiều cửa sổ JavaFX
    private AuctionClient socketClient;
    private boolean isSocketConnected = false;
    private final Map<String, Double> livePrices = new HashMap<>();
    private final Map<String, String> liveBidders = new HashMap<>();

    @FXML
    public void initialize() {
        auctionService = AppContext.getInstance().getAuctionService();
        currentUser = AppContext.getInstance().getCurrentUser();
        setupUserHeader();
        setupTable();
        auctionService.addObserver(this);
        reloadAuctions();
        if (!auctionData.isEmpty()) {
            auctionTable.getSelectionModel().selectFirst();
        }
        connectToSocketServer();
    }

    private void connectToSocketServer() {
        String username = (currentUser != null) ? currentUser.getUsername() : "Guest";
        socketClient = new AuctionClient(username);
        socketClient.setOnMessageReceived(this::handleServerMessage);

        try {
            socketClient.connect("localhost", AuctionServer.PORT);
            isSocketConnected = true;
            logArea.appendText("[Socket] Da ket noi toi AuctionServer (Cong " + AuctionServer.PORT + ")\n");
        } catch (IOException e) {
            isSocketConnected = false;
            logArea.appendText("[Offline] Chua bat AuctionServer, dang chay du lieu cuc bo.\n");
        }
    }

    private void handleServerMessage(Message msg) {
        Platform.runLater(() -> {
            String time = LocalTime.now().format(TIME_FMT);
            switch (msg.getType()) {
                case UPDATE -> {
                    livePrices.put(msg.getAuctionId(), msg.getAmount());
                    liveBidders.put(msg.getAuctionId(), msg.getSender());
                    logArea.appendText(String.format("[%s] [Realtime] %s%n", time, msg.getContent()));
                    auctionTable.refresh();

                    Auction selected = auctionTable.getSelectionModel().getSelectedItem();
                    if (selected != null && selected.getAuctionId().equals(msg.getAuctionId())) {
                        updateSelectedDetails(selected);
                    }
                    if (currentUser != null && currentUser.getUsername().equals(msg.getSender())) {
                        showSuccess("Dat gia thanh cong: " + String.format("%,.0f VND", msg.getAmount()));
                    }
                }
                case ERROR -> showError(msg.getContent());
                case INFO -> logArea.appendText(String.format("[%s] [Server] %s%n", time, msg.getContent()));
                default -> {}
            }
        });
    }

    private double getEffectivePrice(Auction auction) {
        return livePrices.getOrDefault(auction.getAuctionId(), auction.getCurrentPrice());
    }

    private void setupUserHeader() {
        if (currentUser instanceof Bidder b) {
            userInfoLabel.setText(b.getUsername() + " (" + b.getRole() + ") | So du: " + String.format("%,.0f VND", b.getBalance()));
            manageBar.setVisible(false);
            manageBar.setManaged(false);
            createAuctionBtn.setVisible(false);
            createAuctionBtn.setManaged(false);
        } else if (currentUser != null) {
            userInfoLabel.setText(currentUser.getUsername() + " (" + currentUser.getRole() + ")");
            bidAmountField.setDisable(true);
            placeBidBtn.setDisable(true);
            quickBidBtn.setDisable(true);
        }
    }

    private void setupTable() {
        auctionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        idCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAuctionId()));
        itemCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getItem().getName()));
        categoryCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getItem().getCategory()));
        priceCol.setCellValueFactory(c -> new SimpleStringProperty(String.format("%,.0f VND", getEffectivePrice(c.getValue()))));
        bidderCol.setCellValueFactory(c -> {
            String auctionId = c.getValue().getAuctionId();
            if (liveBidders.containsKey(auctionId)) {
                return new SimpleStringProperty(liveBidders.get(auctionId));
            }
            Bidder hb = c.getValue().getHighestBidder();
            return new SimpleStringProperty(hb != null ? hb.getUsername() : "Chua co");
        });
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if (status.equals(AuctionStatus.RUNNING.name())) {
                        setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold;");
                    } else if (status.equals(AuctionStatus.OPEN.name())) {
                        setStyle("-fx-text-fill: #1d4ed8; -fx-font-weight: bold;");
                    } else if (status.equals(AuctionStatus.PAID.name())) {
                        setStyle("-fx-text-fill: #7e22ce; -fx-font-weight: bold;");
                    } else if (status.equals(AuctionStatus.CANCELED.name())) {
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #475569; -fx-font-weight: bold;");
                    }
                }
            }
        });
        auctionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> updateSelectedDetails(selected));
    }

    private void reloadAuctions() {
        Auction prev = auctionTable.getSelectionModel().getSelectedItem();
        auctionData = FXCollections.observableArrayList(auctionService.getAllAuctions());
        auctionTable.setItems(auctionData);
        if (prev != null) {
            for (Auction a : auctionData) {
                if (a.getAuctionId().equals(prev.getAuctionId())) {
                    auctionTable.getSelectionModel().select(a);
                    updateSelectedDetails(a);
                    break;
                }
            }
        }
    }

    private void updateSelectedDetails(Auction selected) {
        if (selected == null) {
            detailTitleLabel.setText("Chua chon phien dau gia");
            itemDetailLabel.setText("");
            shippingLabel.setText("");
            minBidLabel.setText("");
            return;
        }
        double minNext = getEffectivePrice(selected) + selected.getBidIncrement();
        detailTitleLabel.setText(selected.getAuctionId() + " - " + selected.getItem().getName());
        itemDetailLabel.setText(selected.getItem().getItemDetails());
        shippingLabel.setText("Phi van chuyen du kien: " + String.format("%,.0f VND", selected.getItem().calculateShippingFee()));
        minBidLabel.setText("Gia toi thieu tiep theo: " + String.format("%,.0f VND", minNext));
        bidAmountField.setText(String.format("%.0f", minNext));
    }

    @FXML
    private void handlePlaceBid() {
        Auction selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Vui long chon mot phien dau gia");
            return;
        }
        if (!(currentUser instanceof Bidder)) {
            showError("Chi tai khoan BIDDER moi duoc dat gia");
            return;
        }
        try {
            double amount = Double.parseDouble(bidAmountField.getText().replace(",", "").trim());
            if (isSocketConnected) {
                socketClient.placeBid(selected.getAuctionId(), amount);
            } else {
                auctionService.placeBid(selected.getAuctionId(), (Bidder) currentUser, amount);
                showSuccess("Dat gia thanh cong: " + String.format("%,.0f VND", amount));
                updateSelectedDetails(selected);
            }
        } catch (NumberFormatException ex) {
            showError("Vui long nhap so tien hop le");
        } catch (IOException ex) {
            showError("Loi gui lenh len Server: " + ex.getMessage());
        } catch (InvalidBidException | AuctionClosedException | AuthenticationException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleQuickBid() {
        Auction selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Vui long chon mot phien dau gia");
            return;
        }
        if (!(currentUser instanceof Bidder)) {
            showError("Chi tai khoan BIDDER moi duoc dat gia");
            return;
        }
        double nextAmount = getEffectivePrice(selected) + selected.getBidIncrement();
        try {
            if (isSocketConnected) {
                socketClient.placeBid(selected.getAuctionId(), nextAmount);
            } else {
                auctionService.placeBid(selected.getAuctionId(), (Bidder) currentUser, nextAmount);
                showSuccess("Dat nhanh thanh cong: " + String.format("%,.0f VND", nextAmount));
                updateSelectedDetails(selected);
            }
        } catch (IOException ex) {
            showError("Loi gui lenh len Server: " + ex.getMessage());
        } catch (InvalidBidException | AuctionClosedException | AuthenticationException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleStartAuction() {
        Auction selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Chon phien can bat dau");
            return;
        }
        try {
            auctionService.startAuction(selected.getAuctionId());
            showSuccess("Da mo phien " + selected.getAuctionId());
        } catch (AuctionClosedException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleFinishAuction() {
        Auction selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Chon phien can ket thuc");
            return;
        }
        try {
            Bidder winner = auctionService.finishAuction(selected.getAuctionId());
            String winName = winner != null ? winner.getUsername() : "Khong co nguoi dat";
            showSuccess("Ket thuc phien " + selected.getAuctionId() + " | Nguoi thang: " + winName);
        } catch (AuctionClosedException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleMarkPaid() {
        Auction selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Chon phien can xac nhan thanh toan");
            return;
        }
        try {
            auctionService.markAsPaid(selected.getAuctionId());
            showSuccess("Phien " + selected.getAuctionId() + " da chuyen sang PAID");
        } catch (AuctionClosedException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleCancelAuction() {
        Auction selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Chon phien can huy");
            return;
        }
        try {
            auctionService.cancelAuction(selected.getAuctionId());
            showSuccess("Da huy phien " + selected.getAuctionId());
        } catch (AuctionClosedException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleOpenCreateAuction() {
        try {
            AuctionMvcApp.showCreateAuctionModal();
            reloadAuctions();
        } catch (IOException ex) {
            showError("Khong mo duoc form tao phien: " + ex.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        auctionService.removeObserver(this);
        AppContext.getInstance().setCurrentUser(null);
        try {
            AuctionMvcApp.showLogin();
        } catch (IOException ex) {
            showError("Loi dang xuat: " + ex.getMessage());
        }
    }

    private void showSuccess(String msg) {
        feedbackLabel.setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold;");
        feedbackLabel.setText(msg);
    }

    private void showError(String msg) {
        feedbackLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
        feedbackLabel.setText(msg);
    }

    @Override
    public void onNewBid(String auctionId, String bidderName, double newAmount) {
        String msg = String.format("[%s] %s dat %,.0f VND cho %s", LocalTime.now().format(TIME_FMT), bidderName, newAmount, auctionId);
        Platform.runLater(() -> {
            logArea.appendText(msg + "\n");
            auctionTable.refresh();
            Auction selected = auctionTable.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getAuctionId().equals(auctionId)) {
                updateSelectedDetails(selected);
            }
        });
    }

    @Override
    public void onStatusChanged(String auctionId, String newStatus) {
        String msg = String.format("[%s] Phien %s -> %s", LocalTime.now().format(TIME_FMT), auctionId, newStatus);
        Platform.runLater(() -> {
            logArea.appendText(msg + "\n");
            auctionTable.refresh();
        });
    }
}