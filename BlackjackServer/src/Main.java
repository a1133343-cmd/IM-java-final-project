/**
 * 21點伺服器程式進入點
 */
public class Main {
    private static final int DEFAULT_PORT = 12345;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        
        // 可選：從命令列參數讀取 Port
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("無效的 Port 參數，使用預設 Port: " + DEFAULT_PORT);
            }
        }

        BlackjackServer server = new BlackjackServer(port);
        server.start();
    }
}
