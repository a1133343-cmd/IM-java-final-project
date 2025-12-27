import java.io.*;
import java.net.Socket;
import java.util.Map;

/**
 * 客戶端處理器 - 處理單一客戶端連線
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Map<String, GameRoom> rooms;

    private PrintWriter out;
    private BufferedReader in;
    private String uid;
    private String name;
    private GameRoom currentRoom;

    public ClientHandler(Socket socket, Map<String, GameRoom> rooms) {
        this.socket = socket;
        this.rooms = rooms;
    }

    public String getName() {
        return name;
    }

    public String getUid() {
        return uid;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String input;
            while ((input = in.readLine()) != null) {
                processCommand(input);
            }
        } catch (IOException e) {
            System.out.println((name != null ? name : "Unknown") + " 斷線");
        } finally {
            cleanup();
        }
    }

    private void processCommand(String cmd) {
        String[] parts = cmd.split("\\|");
        String action = parts[0];

        switch (action) {
            case Protocol.LOGIN:
                if (parts.length > 2) {
                    this.uid = parts[1];
                    this.name = parts[2];
                } else if (parts.length > 1) {
                    // 向後兼容：若只有 name 沒有 uid
                    this.uid = "unknown";
                    this.name = parts[1];
                } else {
                    this.uid = "unknown";
                    this.name = "Unknown";
                }
                System.out.println("玩家登入 - Name: " + name + ", UID: " + uid);
                send(Protocol.LOGIN_OK);
                break;

            case Protocol.PVE_START:
                startPVE();
                break;

            case Protocol.CREATE_ROOM:
                createRoom();
                break;

            case Protocol.JOIN_ROOM:
                if (parts.length > 1) {
                    joinRoom(parts[1]);
                }
                break;

            case Protocol.START:
                if (currentRoom != null) {
                    currentRoom.tryStartGame(this);
                }
                break;

            case Protocol.READY:
                if (currentRoom != null) {
                    currentRoom.handlePlayerReady(this);
                }
                break;

            case Protocol.CHAT:
                if (currentRoom != null && parts.length > 1) {
                    // 拼接所有訊息部分，避免 | 符號導致訊息被截斷
                    StringBuilder message = new StringBuilder(parts[1]);
                    for (int i = 2; i < parts.length; i++) {
                        message.append("|").append(parts[i]);
                    }
                    currentRoom.broadcastChat(name, message.toString());
                }
                break;

            case Protocol.HIT:
            case Protocol.STAND:
                if (currentRoom != null) {
                    currentRoom.handleGameAction(this, action);
                }
                break;

            case Protocol.USE_FUNCTION_CARD:
                if (currentRoom != null && parts.length > 2) {
                    try {
                        int cardId = Integer.parseInt(parts[1]);
                        String targetUid = parts[2];
                        currentRoom.handleUseFunctionCard(this, cardId, targetUid);
                    } catch (NumberFormatException e) {
                        send(Protocol.ERROR + Protocol.DELIMITER + "無效的功能牌 ID");
                    }
                }
                break;

            case Protocol.SKIP_FUNCTION_CARD:
                if (currentRoom != null) {
                    currentRoom.handleSkipFunctionCard(this);
                }
                break;

            case Protocol.LEAVE:
                leaveRoom();
                break;
        }
    }

    private void startPVE() {
        send(Protocol.PVE_STARTED);
        GameRoom room = new GameRoom(this, null);
        this.currentRoom = room;
        room.startGame();
    }

    private void createRoom() {
        String roomId = generateRoomId();
        GameRoom room = new GameRoom(this, roomId);
        rooms.put(roomId, room);
        this.currentRoom = room;
        send(Protocol.ROOM_CREATED + Protocol.DELIMITER + roomId);
    }

    private String generateRoomId() {
        String roomId;
        do {
            roomId = String.valueOf((int) (Math.random() * 9000) + 1000);
        } while (rooms.containsKey(roomId));
        return roomId;
    }

    private void joinRoom(String roomId) {
        GameRoom room = rooms.get(roomId);
        if (room != null && !room.isFull()) {
            room.addPlayer(this);
            this.currentRoom = room;
            send(Protocol.ROOM_JOINED + Protocol.DELIMITER + roomId);
        } else {
            send(Protocol.ERROR + Protocol.DELIMITER + "房間不存在或已滿");
        }
    }

    private void leaveRoom() {
        if (currentRoom != null) {
            currentRoom.removePlayer(this);
            if (currentRoom.isEmpty() && currentRoom.getRoomId() != null) {
                rooms.remove(currentRoom.getRoomId());
            }
            currentRoom = null;
        }
        send(Protocol.LOBBY);
    }

    private void cleanup() {
        leaveRoom();
        try {
            socket.close();
        } catch (IOException e) {
            // ignore
        }
    }

    public void send(String message) {
        if (out != null) {
            out.println(message);
        }
    }
}
