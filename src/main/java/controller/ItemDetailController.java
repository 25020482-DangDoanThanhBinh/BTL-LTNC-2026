package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import model.Auction;
import model.Bidder;

public class ItemDetailController {
    @FXML
    private Label titleLabel;
    @FXML
    private Label categoryStatusLabel;
    @FXML
    private Label detailsLabel;
    @FXML
    private Label priceLabel;
    @FXML
    private Label incrementLabel;
    @FXML
    private Label shippingLabel;
    @FXML
    private Label leaderLabel;

    public void setAuction(Auction auction) {
        if (auction == null) {
            return;
        }

        titleLabel.setText(auction.getAuctionId() + " - " + auction.getItem().getName());
        categoryStatusLabel.setText("Danh muc: " + auction.getItem().getCategory()
                + "  |  Trang thai: " + auction.getStatus().name());
        detailsLabel.setText(auction.getItem().getItemDetails());
        priceLabel.setText("Gia hien tai: " + String.format("%,.0f VND", auction.getCurrentPrice()));
        incrementLabel.setText("Buoc gia toi thieu: " + String.format("%,.0f VND", auction.getBidIncrement()));
        shippingLabel.setText("Phi van chuyen du kien: "
                + String.format("%,.0f VND", auction.getItem().calculateShippingFee()));

        Bidder hb = auction.getHighestBidder();
        leaderLabel.setText("Nguoi dan dau: " + (hb != null ? hb.getUsername() : "Chua co"));
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) titleLabel.getScene().getWindow();
        stage.close();
    }
}