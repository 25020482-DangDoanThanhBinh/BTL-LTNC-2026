package network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final AuctionServer server;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username = "Ẩn danh";

    public ClientHandler(Socket socket, AuctionServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            // Lưu ý: Luôn tạo ObjectOutputStream trước ObjectInputStream để tránh deadlock
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // Gửi bảng giá hiện tại ngay khi Client vừa kết nối
            sendMessage(new Message(Message.Type.INFO, "", "SERVER", 0, server.getCurrentPricesSummary()));

            Object obj;
            while ((obj = in.readObject()) != null) {
                if (obj instanceof Message msg) {
                    handleMessage(msg);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[Server] " + username + " đã ngắt kết nối.");
        } finally {
            server.removeClient(this);
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private void handleMessage(Message msg) {
        switch (msg.getType()) {
            case JOIN -> {
                this.username = msg.getSender();
                System.out.println("[Server] Người dùng '" + username + "' đã tham gia sàn.");
                server.broadcast(new Message(Message.Type.INFO, "", "SERVER", 0,
                        username + " vừa tham gia phòng đấu giá!"));
            }
            case BID -> server.processBid(msg, this);
            default -> {}
        }
    }

    public synchronized void sendMessage(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            System.err.println("[Server] Không thể gửi tin tới " + username);
        }
    }
}