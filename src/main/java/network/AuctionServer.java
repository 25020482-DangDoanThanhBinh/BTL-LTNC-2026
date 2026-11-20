package network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionServer {
    public static final int PORT = 8888;

    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final Map<String, Double> auctionPrices = new ConcurrentHashMap<>();
    private final Map<String, String> highestBidders = new ConcurrentHashMap<>();
    private final ReentrantLock bidLock = new ReentrantLock();
    private final ExecutorService pool = Executors.newCachedThreadPool();

    public AuctionServer() {
        // Khởi tạo dữ liệu giá ban đầu cho các phiên mẫu
        auctionPrices.put("AUC-101", 30_000_000.0);
        auctionPrices.put("AUC-103", 45_000_000.0);
    }

    public void start() {
        System.out.println("=== AUCTION SERVER ĐANG CHẠY TẠI CỔNG " + PORT + " ===");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Server] Client mới kết nối: " + clientSocket.getRemoteSocketAddress());
                ClientHandler handler = new ClientHandler(clientSocket, this);
                clients.add(handler);
                pool.execute(handler);
            }
        } catch (IOException e) {
            System.err.println("[Server] Lỗi khởi động: " + e.getMessage());
        } finally {
            pool.shutdown();
        }
    }

    // Xử lý đặt giá an toàn đa luồng và phát sóng (Broadcast) cho toàn bộ Client
    public void processBid(Message bidMsg, ClientHandler senderHandler) {
        bidLock.lock();
        try {
            String auctionId = bidMsg.getAuctionId();
            double amount = bidMsg.getAmount();
            String bidder = bidMsg.getSender();

            if (!auctionPrices.containsKey(auctionId)) {
                senderHandler.sendMessage(new Message(
                        Message.Type.ERROR, auctionId, "SERVER", 0,
                        "Không tìm thấy phiên đấu giá: " + auctionId));
                return;
            }

            double currentPrice = auctionPrices.get(auctionId);
            double minNext = currentPrice + 500_000;

            if (amount < minNext) {
                senderHandler.sendMessage(new Message(
                        Message.Type.ERROR, auctionId, "SERVER", currentPrice,
                        String.format("Giá %,.0f VND quá thấp! Mức tối thiểu: %,.0f VND", amount, minNext)));
                return;
            }

            // Cập nhật giá mới
            auctionPrices.put(auctionId, amount);
            highestBidders.put(auctionId, bidder);

            String updateText = String.format("Phiên %s có giá mới: %,.0f VND (bởi %s)",
                    auctionId, amount, bidder);
            System.out.println("[Server] " + updateText);

            // Phát sóng cho TẤT CẢ Client đang kết nối
            broadcast(new Message(Message.Type.UPDATE, auctionId, bidder, amount, updateText));
        } finally {
            bidLock.unlock();
        }
    }

    public void broadcast(Message msg) {
        for (ClientHandler client : clients) {
            client.sendMessage(msg);
        }
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    public String getCurrentPricesSummary() {
        StringBuilder sb = new StringBuilder("Danh sách phiên hiện tại:\n");
        auctionPrices.forEach((id, price) -> {
            String leader = highestBidders.getOrDefault(id, "Chưa có");
            sb.append(String.format(" - %s | Giá: %,.0f VND | Dẫn đầu: %s%n", id, price, leader));
        });
        return sb.toString();
    }

    public static void main(String[] args) {
        new AuctionServer().start();
    }
}