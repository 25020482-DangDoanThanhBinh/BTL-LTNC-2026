package network;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        JOIN,              // Client đăng nhập vào sàn
        BID,               // Client gửi lệnh đặt giá
        UPDATE,            // Server phát sóng (broadcast) giá mới cho tất cả Client
        ERROR,             // Server báo lỗi (giá thấp, phiên đóng...)
        INFO               // Thông báo chung từ Server
    }

    private final Type type;
    private final String auctionId;
    private final String sender;
    private final double amount;
    private final String content;

    public Message(Type type, String auctionId, String sender, double amount, String content) {
        this.type = type;
        this.auctionId = auctionId;
        this.sender = sender;
        this.amount = amount;
        this.content = content;
    }

    public Type getType() { return type; }
    public String getAuctionId() { return auctionId; }
    public String getSender() { return sender; }
    public double getAmount() { return amount; }
    public String getContent() { return content; }
}