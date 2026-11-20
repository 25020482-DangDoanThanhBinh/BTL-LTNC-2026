package ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import model.AuctionStatus;
import observer.AuctionObserver;
import observer.BidConsoleObserver;
import service.AuctionEngine;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class AuctionGuiApp extends Application {
    private Stage primaryStage;
    private ObservableList<AuctionEngine> auctionData;
    private TextArea liveLogArea;
    private TableView<AuctionEngine> auctionTable;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        initSampleData();
        showLoginScreen();
    }

    private void initSampleData() {
        AuctionEngine a1 = new AuctionEngine("AUC-01", "MacBook Pro M3 Max", 30_000_000, 500_000);
        AuctionEngine a2 = new AuctionEngine("AUC-02", "Tranh Sơn Dầu Cổ 1960", 15_000_000, 500_000);
        AuctionEngine a3 = new AuctionEngine("AUC-03", "Xe Máy Cổ Honda 67", 45_000_000, 1_000_000);
        AuctionEngine a4 = new AuctionEngine("AUC-04", "Đồng Hồ Omega Seamaster", 80_000_000, 2_000_000);

        BidConsoleObserver consoleObs = new BidConsoleObserver("System");
        AuctionObserver uiObs = new AuctionObserver() {
            @Override
            public void onNewBid(String auctionId, String bidderName, double newAmount) {
                appendLog(String.format("[%s] %s đặt %,.0f VND cho phiên %s",
                        LocalTime.now().format(TIME_FMT), bidderName, newAmount, auctionId));
                Platform.runLater(() -> {
                    if (auctionTable != null) auctionTable.refresh();
                });
            }

            @Override
            public void onStatusChanged(String auctionId, String newStatus) {
                appendLog(String.format("[%s] Phiên %s chuyển trạng thái -> %s",
                        LocalTime.now().format(TIME_FMT), auctionId, newStatus));
                Platform.runLater(() -> {
                    if (auctionTable != null) auctionTable.refresh();
                });
            }
        };

        auctionData = FXCollections.observableArrayList(a1, a2, a3, a4);
        for (AuctionEngine a : auctionData) {
            a.addObserver(consoleObs);
            a.addObserver(uiObs);
        }

        a1.startAuction();
        a3.startAuction();
        a4.startAuction();
        a4.finishAuction();
    }

    // ==================== MÀN HÌNH 1: ĐĂNG NHẬP ====================
    private void showLoginScreen() {
        Label brand = new Label("AUCTION PRO");
        brand.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label subtitle = new Label("Hệ thống đấu giá trực tuyến thời gian thực");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        TextField userField = new TextField();
        userField.setPromptText("Tên đăng nhập (vd: chung427)");
        userField.setStyle("-fx-padding: 10; -fx-background-radius: 6; -fx-border-color: #cbd5e1; -fx-border-radius: 6;");

        PasswordField passField = new PasswordField();
        passField.setPromptText("Mật khẩu");
        passField.setStyle("-fx-padding: 10; -fx-background-radius: 6; -fx-border-color: #cbd5e1; -fx-border-radius: 6;");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");

        Button loginBtn = new Button("Đăng nhập vào sàn");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 10; -fx-background-radius: 6; -fx-cursor: hand;");

        loginBtn.setOnAction(e -> {
            String username = userField.getText().trim();
            if (username.isEmpty()) {
                errorLabel.setText("Vui lòng nhập tên đăng nhập để tiếp tục.");
            } else {
                showDashboardScreen(username);
            }
        });

        // Cho phép nhấn Enter để đăng nhập
        passField.setOnAction(e -> loginBtn.fire());
        userField.setOnAction(e -> loginBtn.fire());

        VBox card = new VBox(12, brand, subtitle, new Separator(), userField, passField, loginBtn, errorLabel);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(30));
        card.setMaxWidth(360);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 18, 0, 0, 4);");

        StackPane root = new StackPane(card);
        root.setStyle("-fx-background-color: #f1f5f9;");
        root.setPadding(new Insets(30));

        primaryStage.setTitle("Auction Pro - Đăng nhập");
        primaryStage.setScene(new Scene(root, 480, 420));
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    // ==================== MÀN HÌNH 2: DASHBOARD ĐẤU GIÁ ====================
    private void showDashboardScreen(String username) {
        // 1. Header Bar
        Label logo = new Label("AUCTION PRO");
        logo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label userLabel = new Label("Người dùng: " + username);
        userLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13px; -fx-font-weight: bold;");

        Button logoutBtn = new Button("Đăng xuất");
        logoutBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 6; -fx-padding: 6 14; -fx-cursor: hand;");
        logoutBtn.setOnAction(e -> showLoginScreen());

        HBox header = new HBox(15, logo, spacer, userLabel, logoutBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 24, 14, 24));
        header.setStyle("-fx-background-color: #0f172a;");

        // 2. Center: Bảng danh sách phiên đấu giá (TableView)
        auctionTable = createAuctionTable();
        auctionTable.setItems(auctionData);
        auctionTable.getSelectionModel().selectFirst();

        Label tableTitle = new Label("Danh sách phiên đấu giá trực tuyến");
        tableTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        VBox centerBox = new VBox(12, tableTitle, auctionTable);
        centerBox.setPadding(new Insets(20));
        VBox.setVgrow(auctionTable, Priority.ALWAYS);

        // 3. Right Panel: Đặt giá + Nhật ký Realtime
        VBox rightPanel = createBiddingPanel(username);

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(centerBox);
        root.setRight(rightPanel);
        root.setStyle("-fx-background-color: #f8fafc;");

        primaryStage.setTitle("Auction Pro - Sàn đấu giá trực tuyến");
        primaryStage.setScene(new Scene(root, 960, 560));
        primaryStage.centerOnScreen();
    }

    private TableView<AuctionEngine> createAuctionTable() {
        TableView<AuctionEngine> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setStyle("-fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

        TableColumn<AuctionEngine, String> idCol = new TableColumn<>("Mã phiên");
        idCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAuctionId()));
        idCol.setMaxWidth(100);

        TableColumn<AuctionEngine, String> nameCol = new TableColumn<>("Sản phẩm");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getItemName()));

        TableColumn<AuctionEngine, String> priceCol = new TableColumn<>("Giá hiện tại");
        priceCol.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("%,.0f VND", c.getValue().getCurrentPrice())));

        TableColumn<AuctionEngine, String> bidderCol = new TableColumn<>("Người dẫn đầu");
        bidderCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHighestBidder()));

        TableColumn<AuctionEngine, String> statusCol = new TableColumn<>("Trạng thái");
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
                    } else {
                        setStyle("-fx-text-fill: #64748b; -fx-font-weight: bold;");
                    }
                }
            }
        });

        table.getColumns().addAll(idCol, nameCol, priceCol, bidderCol, statusCol);
        return table;
    }

    private VBox createBiddingPanel(String username) {
        Label panelTitle = new Label("Đặt giá nhanh");
        panelTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label selectedInfo = new Label("Chọn 1 phiên ở bảng bên trái");
        selectedInfo.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px;");
        selectedInfo.setWrapText(true);

        TextField bidAmountField = new TextField();
        bidAmountField.setPromptText("Nhập số tiền muốn đặt (VND)");
        bidAmountField.setStyle("-fx-padding: 8; -fx-background-radius: 6; -fx-border-color: #cbd5e1; -fx-border-radius: 6;");

        Label feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackLabel.setStyle("-fx-font-size: 12px;");

        Button bidBtn = new Button("Xác nhận đặt giá");
        bidBtn.setMaxWidth(Double.MAX_VALUE);
        bidBtn.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 10; -fx-background-radius: 6; -fx-cursor: hand;");

        Button quickStepBtn = new Button("+1 bước giá tối thiểu");
        quickStepBtn.setMaxWidth(Double.MAX_VALUE);
        quickStepBtn.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #1e293b; -fx-font-weight: bold; " +
                "-fx-padding: 8; -fx-background-radius: 6; -fx-cursor: hand;");

        // Cập nhật gợi ý khi người dùng bấm chọn dòng khác trên bảng
        auctionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                double minNext = selected.getCurrentPrice() + selected.getBidIncrement();
                selectedInfo.setText(String.format("%s - %s\nTối thiểu tiếp theo: %,.0f VND",
                        selected.getAuctionId(), selected.getItemName(), minNext));
                bidAmountField.setText(String.format("%.0f", minNext));
                feedbackLabel.setText("");
            }
        });

        quickStepBtn.setOnAction(e -> {
            AuctionEngine selected = auctionTable.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            double nextPrice = selected.getCurrentPrice() + selected.getBidIncrement();
            handleBid(selected, username, nextPrice, feedbackLabel, bidAmountField, selectedInfo);
        });

        bidBtn.setOnAction(e -> {
            AuctionEngine selected = auctionTable.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            try {
                double amount = Double.parseDouble(bidAmountField.getText().replace(",", "").trim());
                handleBid(selected, username, amount, feedbackLabel, bidAmountField, selectedInfo);
            } catch (NumberFormatException ex) {
                feedbackLabel.setStyle("-fx-text-fill: #dc2626;");
                feedbackLabel.setText("Vui lòng nhập số tiền hợp lệ!");
            }
        });

        Label logTitle = new Label("Nhật ký trực tiếp (Observer)");
        logTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        liveLogArea = new TextArea();
        liveLogArea.setEditable(false);
        liveLogArea.setWrapText(true);
        liveLogArea.setPrefHeight(200);
        liveLogArea.setStyle("-fx-font-family: 'Monospaced'; -fx-font-size: 11px;");
        VBox.setVgrow(liveLogArea, Priority.ALWAYS);

        VBox panel = new VBox(10, panelTitle, selectedInfo, bidAmountField, bidBtn, quickStepBtn,
                feedbackLabel, new Separator(), logTitle, liveLogArea);
        panel.setPadding(new Insets(20));
        panel.setPrefWidth(310);
        panel.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 0 1;");
        return panel;
    }

    private void handleBid(AuctionEngine selected, String username, double amount,
                           Label feedbackLabel, TextField bidAmountField, Label selectedInfo) {
        boolean ok = selected.placeBid(username, amount);
        if (ok) {
            feedbackLabel.setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold;");
            feedbackLabel.setText("Đặt giá thành công!");
            double minNext = selected.getCurrentPrice() + selected.getBidIncrement();
            bidAmountField.setText(String.format("%.0f", minNext));
            selectedInfo.setText(String.format("%s - %s\nTối thiểu tiếp theo: %,.0f VND",
                    selected.getAuctionId(), selected.getItemName(), minNext));
        } else {
            feedbackLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
            feedbackLabel.setText("Từ chối: Phiên không ở trạng thái RUNNING hoặc giá quá thấp!");
        }
    }

    private void appendLog(String message) {
        Platform.runLater(() -> {
            if (liveLogArea != null) {
                liveLogArea.appendText(message + "\n");
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}