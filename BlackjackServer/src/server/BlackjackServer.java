import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 21點伺服器主控制器
 */
public class BlackjackServer {
    private final int port;
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();

    public BlackjackServer(int port) {
        this.port = port;
    }

    /**
     * 啟動伺服器
     */
    public void start() {
        System.out.println("=== Blackjack Server (PVP 莊家消極懲罰版) Port: " + port + " ===");
        
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("新連線: " + socket.getInetAddress().getHostAddress());
                
                ClientHandler client = new ClientHandler(socket, rooms);
                new Thread(client).start();
            }
        } catch (IOException e) {
            System.err.println("伺服器錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Map<String, GameRoom> getRooms() {
        return rooms;
    }
}
