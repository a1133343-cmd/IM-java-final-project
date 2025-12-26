import java.io.*;
import java.net.Socket;

/**
 * 網路客戶端 - 管理與伺服器的連線
 */
public class NetworkClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private MessageHandler messageHandler;

    public void setMessageHandler(MessageHandler handler) {
        this.messageHandler = handler;
    }

    /**
     * 連線到伺服器
     */
    public void connect(String host, int port, String playerName) throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        // 發送登入指令
        send(Protocol.LOGIN + Protocol.DELIMITER + playerName);
        
        // 啟動監聽執行緒
        startListening();
    }

    /**
     * 發送訊息到伺服器
     */
    public void send(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    /**
     * 關閉連線
     */
    public void close() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            // ignore
        }
    }

    private void startListening() {
        new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    if (messageHandler != null) {
                        messageHandler.handleMessage(msg);
                    }
                }
            } catch (IOException e) {
                if (messageHandler != null) {
                    messageHandler.onDisconnected();
                }
            }
        }).start();
    }
}
