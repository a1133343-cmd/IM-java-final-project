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
    private String name;
    private GameRoom currentRoom;

    public ClientHandler(Socket socket, Map<String, GameRoom> rooms) {
        this.socket = socket;
        this.rooms = rooms;
    }

    public String getName() {
        return name;
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
                this.name = parts.length > 1 ? parts[1] : "Unknown";
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
                    currentRoom.broadcastChat(name, parts[1]);
                }
                break;
                
            case Protocol.HIT:
            case Protocol.STAND:
                if (currentRoom != null) {
                    currentRoom.handleGameAction(this, action);
                }
                break;
                
            case Protocol.LEAVE:
                leaveRoom();
                break;
        }
    }

    private void startPVE() {
        send(Protocol.PVE_STARTED);
        GameRoom room = new GameRoom(this, null, rooms);
        this.currentRoom = room;
        room.startGame();
    }

    private void createRoom() {
        String roomId = generateRoomId();
        GameRoom room = new GameRoom(this, roomId, rooms);
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
