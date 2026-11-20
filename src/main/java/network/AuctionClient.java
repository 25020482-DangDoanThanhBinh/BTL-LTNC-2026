package network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.function.Consumer;

public class AuctionClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final String username;
    private Consumer<Message> onMessageReceived;

    public AuctionClient(String username) {
        this.username = username;
    }

    public void setOnMessageReceived(Consumer<Message> callback) {
        this.onMessageReceived = callback;
    }

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        // Gửi thông báo tham gia
        send(new Message(Message.Type.JOIN, "", username, 0, "Joined"));

        // Luồng nền lắng nghe gói tin từ Server gửi về
        Thread listener = new Thread(() -> {
            try {
                Object obj;
                while ((obj = in.readObject()) != null) {
                    if (obj instanceof Message msg && onMessageReceived != null) {
                        onMessageReceived.accept(msg);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("[Client] Đã mất kết nối với Server.");
            }
        });
        listener.setDaemon(true);
        listener.start();
    }

    public void placeBid(String auctionId, double amount) throws IOException {
        send(new Message(Message.Type.BID, auctionId, username, amount, ""));
    }

    private synchronized void send(Message msg) throws IOException {
        out.writeObject(msg);
        out.flush();
    }

    // Hàm main để chạy thử 2-3 Client cùng lúc kiểm tra Realtime Broadcast
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Nhập tên đăng nhập của bạn: ");
        String name = sc.nextLine().trim();

        AuctionClient client = new AuctionClient(name);
        client.setOnMessageReceived(msg -> {
            System.out.println("\n>>> [" + msg.getType() + "] " + msg.getContent());
            System.out.print("Nhập lệnh (<MaPhien> <SoTien>, vd: AUC-101 31000000): ");
        });

        try {
            client.connect("localhost", AuctionServer.PORT);
            System.out.println("Kết nối thành công! Nhập 'exit' để thoát.");

            while (true) {
                String line = sc.nextLine().trim();
                if (line.equalsIgnoreCase("exit")) break;

                String[] parts = line.split("\\s+");
                if (parts.length == 2) {
                    try {
                        String auctionId = parts[0].toUpperCase();
                        double amount = Double.parseDouble(parts[1]);
                        client.placeBid(auctionId, amount);
                    } catch (NumberFormatException e) {
                        System.out.println("Số tiền không hợp lệ!");
                    }
                } else {
                    System.out.println("Sai cú pháp! Hãy nhập: AUC-101 31000000");
                }
            }
        } catch (IOException e) {
            System.err.println("Không thể kết nối Server (hãy chắc chắn đã chạy AuctionServer trước!): " + e.getMessage());
        }
    }
}