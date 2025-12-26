import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class main {
    private static final int PORT = 12345;
    private static Map<String, GameRoom> rooms = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("=== Blackjack Server (PVP 莊家消極懲罰版) Port: " + PORT + " ===");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler client = new ClientHandler(socket);
                new Thread(client).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- ClientHandler ---
    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        public String name;
        private GameRoom currentRoom;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String input;
                while ((input = in.readLine()) != null)
                    processCommand(input);
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
                case "LOGIN":
                    this.name = parts.length > 1 ? parts[1] : "Unknown";
                    send("LOGIN_OK");
                    break;
                case "PVE_START":
                    startPVE();
                    break;
                case "CREATE_ROOM":
                    createRoom();
                    break;
                case "JOIN_ROOM":
                    if (parts.length > 1)
                        joinRoom(parts[1]);
                    break;
                case "START":
                    if (currentRoom != null)
                        currentRoom.tryStartGame(this);
                    break;
                case "READY":
                    if (currentRoom != null)
                        currentRoom.handlePlayerReady(this);
                    break;
                case "CHAT":
                    if (currentRoom != null && parts.length > 1)
                        currentRoom.broadcastChat(name, parts[1]);
                    break;
                case "HIT":
                case "STAND":
                    if (currentRoom != null)
                        currentRoom.handleGameAction(this, action);
                    break;
                case "LEAVE":
                    leaveRoom();
                    break;
            }
        }

        private void startPVE() {
            send("PVE_STARTED"); 
            GameRoom room = new GameRoom(this, null);
            this.currentRoom = room;
            room.startGame();
        }

        private void createRoom() {
            String roomId = String.valueOf((int) (Math.random() * 9000) + 1000);
            while (rooms.containsKey(roomId)) {
                roomId = String.valueOf((int) (Math.random() * 9000) + 1000);
            }
            GameRoom room = new GameRoom(this, roomId);
            rooms.put(roomId, room);
            this.currentRoom = room;
            send("ROOM_CREATED|" + roomId);
        }

        private void joinRoom(String roomId) {
            GameRoom room = rooms.get(roomId);
            if (room != null && !room.isFull()) {
                room.addPlayer(this);
                this.currentRoom = room;
                send("ROOM_JOINED|" + roomId);
            } else {
                send("ERROR|房間不存在或已滿");
            }
        }

        private void leaveRoom() {
            if (currentRoom != null) {
                currentRoom.removePlayer(this);
                if (currentRoom.isEmpty() && currentRoom.roomId != null) {
                    rooms.remove(currentRoom.roomId);
                }
                currentRoom = null;
            }
            send("LOBBY");
        }

        private void cleanup() {
            leaveRoom();
            try {
                socket.close();
            } catch (IOException e) {
            }
        }

        public void send(String msg) {
            if (out != null)
                out.println(msg);
        }
    }

    // --- PlayerInfo ---
    static class PlayerInfo {
        ClientHandler handler;
        Hand hand;
        int hp;
        boolean isDealer;
        boolean hasStayed;
        boolean isReady;

        public PlayerInfo(ClientHandler h) {
            this.handler = h;
            this.hp = 15;
            this.hand = new Hand();
            this.isDealer = false;
            this.hasStayed = false;
            this.isReady = true;
        }

        void resetHand() {
            hand = new Hand();
            hasStayed = false;
        }
    }

    // --- GameRoom ---
    static class GameRoom {
        String roomId;
        List<PlayerInfo> players = new ArrayList<>();
        Deck deck;

        int dealerIndex = 0;
        int turnIndex = 0;
        boolean gameInProgress = false;

        public GameRoom(ClientHandler creator, String id) {
            this.roomId = id;
            addPlayer(creator);
        }

        public boolean isFull() {
            return players.size() >= 5;
        }

        public boolean isEmpty() {
            return players.isEmpty();
        }

        public void addPlayer(ClientHandler h) {
            if (!isFull()) {
                PlayerInfo newPlayer = new PlayerInfo(h);
                if (players.isEmpty()) {
                    newPlayer.isDealer = true;
                    dealerIndex = 0;
                }
                players.add(newPlayer);
                broadcast("MSG|玩家 " + h.name + " 加入 (" + players.size() + "/5)");
                broadcast("HP_UPDATE|" + getHpString());
            }
        }

        public void removePlayer(ClientHandler h) {
            players.removeIf(p -> p.handler == h);
            broadcast("MSG|玩家 " + h.name + " 離開");

            if (!players.isEmpty()) {
                if (dealerIndex >= players.size())
                    dealerIndex = 0;
                for (int i = 0; i < players.size(); i++) {
                    players.get(i).isDealer = (i == dealerIndex);
                }
            }

            broadcast("HP_UPDATE|" + getHpString());

            if (gameInProgress && players.size() < 1 && roomId != null) {
                gameInProgress = false;
                broadcast("MSG|人數不足，遊戲結束");
            }
        }

        public void handlePlayerReady(ClientHandler h) {
            boolean allReady = true;
            for (PlayerInfo p : players) {
                if (p.handler == h) {
                    p.isReady = true;
                    broadcast("MSG|" + p.handler.name + " 已確認戰績");
                }
                if (!p.isReady)
                    allReady = false;
            }

            if (allReady && players.size() > 1) {
                broadcast("MSG|所有玩家已確認完畢，莊家可開始下一局");
            }
        }

        public void broadcastChat(String sender, String msg) {
            broadcast("CHAT|" + sender + ": " + msg);
        }

        private void broadcast(String msg) {
            for (PlayerInfo p : players)
                if (p.handler != null)
                    p.handler.send(msg);
        }

        public void tryStartGame(ClientHandler requestor) {
            if (players.isEmpty())
                return;

            if (roomId == null) {
                startGame();
                return;
            }

            if (dealerIndex >= players.size())
                dealerIndex = 0;

            if (players.get(dealerIndex).handler == requestor) {
                List<String> notReadyList = new ArrayList<>();
                for (PlayerInfo p : players) {
                    if (!p.isReady) {
                        notReadyList.add(p.handler.name);
                    }
                }

                if (!notReadyList.isEmpty()) {
                    requestor.send("ERROR|無法開始！以下玩家尚未確認戰績: " + String.join(", ", notReadyList));
                } else {
                    startGame();
                }
            } else {
                requestor.send("ERROR|只有當前莊家 (" + players.get(dealerIndex).handler.name + ") 可以開始");
            }
        }

        public void startGame() {
            if (roomId != null && players.size() < 1) {
                broadcast("MSG|至少需要 1 人才能開始");
                return;
            }

            gameInProgress = true;
            deck = new Deck();
            deck.shuffle();

            if (roomId == null) {
                startPVE();
                return;
            }

            if (dealerIndex >= players.size())
                dealerIndex = 0;

            for (int i = 0; i < players.size(); i++) {
                PlayerInfo p = players.get(i);
                p.resetHand();
                p.isDealer = (i == dealerIndex);
                p.hand.add(deck.draw());
                p.hand.add(deck.draw());
            }

            broadcast("MSG|=== 新局開始，莊家是 " + players.get(dealerIndex).handler.name + " ===");
            turnIndex = (dealerIndex + 1) % players.size();

            sendStateToAll();
            checkAndNotifyTurn();
        }

        private void startPVE() {
            PlayerInfo p = players.get(0);
            p.resetHand();
            p.isDealer = false;
            p.hand.add(deck.draw());
            p.hand.add(deck.draw());

            PlayerInfo cpu = new PlayerInfo(null);
            cpu.isDealer = true;
            cpu.hand.add(deck.draw());
            cpu.hand.add(deck.draw());

            if (players.size() > 1)
                players.remove(1);
            players.add(cpu);
            turnIndex = 0;
            sendStateToAll();
            p.handler.send("TURN|YOUR");
        }

        private void checkAndNotifyTurn() {
            PlayerInfo current = players.get(turnIndex);
            int checkCount = 0;
            while (current.hasStayed || current.hand.isBust()) {
                if (current.isDealer) {
                    endRound();
                    return;
                }
                turnIndex = (turnIndex + 1) % players.size();
                current = players.get(turnIndex);
                checkCount++;
                if (checkCount > players.size() + 1) {
                    endRound();
                    return;
                }
            }

            for (PlayerInfo p : players) {
                if (p == current) {
                    p.handler.send("TURN|YOUR");
                    p.handler.send("MSG|輪到你了！");
                } else {
                    p.handler.send("TURN|WAIT");
                    if (p.isDealer)
                        p.handler.send("MSG|等待閒家行動...");
                    else
                        p.handler.send("MSG|等待 " + current.handler.name + " 行動...");
                }
            }
        }

        public void handleGameAction(ClientHandler h, String action) {
            if (!gameInProgress)
                return;
            PlayerInfo currentP = players.get(turnIndex);

            if (roomId == null) {
                handlePVE(h, action);
                return;
            }
            if (currentP.handler != h) {
                h.send("MSG|還沒輪到你！");
                return;
            }

            if (action.equals("HIT")) {
                currentP.hand.add(deck.draw());
                if (currentP.hand.isBust()) {
                    h.send("MSG|爆牌了！");
                    currentP.hasStayed = true;
                    nextTurn();
                } else {
                    sendStateToAll();
                    h.send("TURN|YOUR");
                }
            } else if (action.equals("STAND")) {
                currentP.hasStayed = true;
                h.send("MSG|你選擇了停牌");
                nextTurn();
            }
        }

        private void handlePVE(ClientHandler h, String action) {
            PlayerInfo p = players.get(0);
            PlayerInfo cpu = players.get(1);

            if (action.equals("HIT")) {
                p.hand.add(deck.draw());
                if (p.hand.isBust()) {
                    p.handler.send("GAME_OVER|" + cpu.hand.toString(true, false) + "|" + p.hand.toString(true, false)
                            + "|你爆牌了");
                    gameInProgress = false;
                } else {
                    sendStateToAll();
                }
            } else {
                while (cpu.hand.bestValue() < 17)
                    cpu.hand.add(deck.draw());
                int pVal = p.hand.bestValue();
                int cVal = cpu.hand.bestValue();
                boolean cpuBust = cpu.hand.isBust();
                String res;
                if (cpuBust)
                    res = "電腦爆牌，你贏了";
                else if (pVal > cVal)
                    res = "你點數較大，贏了";
                else if (pVal < cVal) {
                    res = "電腦點數較大，輸了";
                } else
                    res = "平手";

                p.handler.send(
                        "GAME_OVER|" + cpu.hand.toString(true, false) + "|" + p.hand.toString(true, false) + "|" + res);
                gameInProgress = false;
            }
            if (!gameInProgress) {
                p.handler.send("HP_UPDATE|" + getHpString());
            }
        }

        private void nextTurn() {
            sendStateToAll();
            PlayerInfo current = players.get(turnIndex);
            if (current.isDealer && (current.hasStayed || current.hand.isBust())) {
                endRound();
                return;
            }
            turnIndex = (turnIndex + 1) % players.size();
            checkAndNotifyTurn();
        }

        private void endRound() {
            PlayerInfo dealer = players.get(dealerIndex);
            int dScore = dealer.hand.bestValue();
            boolean dBust = dealer.hand.isBust();

            // [新增規則] 判斷莊家是否拿過牌 (大於2張表示有HIT過)
            boolean dealerHit = (dealer.hand.cards.size() > 2);
            // [新增規則] 統計莊家贏了幾個人
            int dealerWinCount = 0;

            StringBuilder sb = new StringBuilder("本局結算:\\n");
            if (dBust) {
                dealer.hp -= 2;
                sb.append("[莊] ").append(dealer.handler.name).append(" 爆牌 (-2 HP)\\n");
            } else {
                sb.append("[莊] ").append(dealer.handler.name).append(" ").append(dScore).append("點\\n");
            }

            for (int i = 0; i < players.size(); i++) {
                if (i == dealerIndex)
                    continue;
                PlayerInfo p = players.get(i);
                int pScore = p.hand.bestValue();

                if (p.hand.isBust()) {
                    dealerWinCount++; // 閒家爆牌，莊家算贏
                    p.hp -= 1;
                    sb.append(p.handler.name).append(" 爆牌 (-1 HP)\\n");
                } else if (!dBust && pScore < dScore) {
                    dealerWinCount++; // 閒家點數小，莊家算贏
                    p.hp -= 1;
                    sb.append(p.handler.name).append(" 輸莊家 (-1 HP)\\n");
                } else if (!dBust && pScore > dScore) {
                    sb.append(p.handler.name).append(" 贏莊家\\n");
                } else {
                    sb.append(p.handler.name).append(" 平手\\n");
                }
            }

            // [新增規則] 實作：莊家不拿牌 (dealerHit為false) 且 沒有贏任何人 (dealerWinCount == 0)
            if (!dBust && !dealerHit && dealerWinCount == 0) {
                dealer.hp -= 1;
                sb.append(">>> [莊] 消極避戰且未獲勝，懲罰 (-1 HP) <<<\\n");
            }

            for (PlayerInfo p : players) {
                String dHand = dealer.hand.toString(true, false);
                String mHand = p.hand.toString(true, false);
                p.handler.send("GAME_OVER|" + dHand + "|" + mHand + "|" + sb.toString());
            }

            for (PlayerInfo p : players) {
                p.isReady = false;
            }

            List<PlayerInfo> deadPlayers = new ArrayList<>();
            players.removeIf(p -> {
                if (p.hp <= 0) {
                    deadPlayers.add(p);
                    return true;
                }
                return false;
            });

            for (PlayerInfo dead : deadPlayers) {
                dead.handler.send("MSG|你的血量歸零，被淘汰了！");
                dead.handler.send("LOBBY");
            }

            gameInProgress = false;

            if (players.size() == 1) {
                dealerIndex = 0;
                players.get(0).isDealer = true;
                players.get(0).isReady = true;
                broadcast("HP_UPDATE|" + getHpString());
            } else if (players.size() > 1) {
                dealerIndex = (dealerIndex + 1) % players.size();
                for (int i = 0; i < players.size(); i++) {
                    players.get(i).isDealer = (i == dealerIndex);
                }

                broadcast("HP_UPDATE|" + getHpString());
                broadcast("MSG|請所有玩家確認結算視窗，以進行下一局...");
            }
        }

        private String getHpString() {
            StringBuilder sb = new StringBuilder();
            for (PlayerInfo p : players) {
                if (p.handler != null)
                    sb.append(p.handler.name).append(p.isDealer ? "(莊)" : "").append(":").append(p.hp).append("HP,");
            }
            return sb.toString();
        }

        private String getSuitSymbol(String suit) {
            switch (suit) {
                case "H": return "♥";
                case "D": return "♦";
                case "C": return "♣";
                case "S": return "♠";
                default: return suit;
            }
        }

        private String formatHandForList(Hand h) {
            StringBuilder sb = new StringBuilder(" [");
            for(int i=0; i<h.cards.size(); i++) {
                Card c = h.cards.get(i);
                sb.append(getSuitSymbol(c.suit)).append(c.rank);
                if(i < h.cards.size()-1) sb.append(" ");
            }
            sb.append("] ").append(h.bestValue()).append("點");
            return sb.toString();
        }

        private void sendStateToAll() {
            if (roomId == null) {
                PlayerInfo p = players.get(0);
                PlayerInfo cpu = players.get(1);
                p.handler.send("STATE|PLAYING|" + cpu.hand.toString(false, true) + "|" + p.hand.toString(true, false)
                        + "|CPU:??,YOU:" + p.hp);
                return;
            }

            PlayerInfo dealer = players.get(dealerIndex);

            StringBuilder normalListSb = new StringBuilder();
            for (PlayerInfo p : players) {
                normalListSb.append(p.handler.name)
                            .append(p.isDealer ? "(莊)" : "")
                            .append(":").append(p.hp).append("HP,");
            }
            String normalListStr = normalListSb.toString();

            StringBuilder dealerListSb = new StringBuilder();
            for (PlayerInfo p : players) {
                dealerListSb.append(p.handler.name)
                            .append(p.isDealer ? "(莊)" : "")
                            .append(":").append(p.hp).append("HP");
                
                if (!p.isDealer) {
                    dealerListSb.append(formatHandForList(p.hand));
                }
                dealerListSb.append(",");
            }
            String dealerListStr = dealerListSb.toString();

            for (PlayerInfo p : players) {
                String dHand = (p == dealer) ? dealer.hand.toString(true, false) : dealer.hand.toString(false, true);
                String mHand = p.hand.toString(true, false);
                
                String listData = (p == dealer) ? dealerListStr : normalListStr;

                p.handler.send("STATE|PLAYING|" + dHand + "|" + mHand + "|" + listData);
            }
        }
    }

    // --- Utils (Deck, Card, Hand 保持不變) ---
    static class Deck {
        List<Card> cards = new ArrayList<>();
        int idx = 0;

        Deck() {
            String[] suits = { "H", "D", "C", "S" };
            String[] ranks = { "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A" };
            for (String s : suits)
                for (String r : ranks)
                    cards.add(new Card(s, r));
        }

        void shuffle() {
            Collections.shuffle(cards);
            idx = 0;
        }

        Card draw() {
            if (idx >= cards.size())
                shuffle();
            return cards.get(idx++);
        }
    }

    static class Card {
        String suit, rank;

        Card(String s, String r) {
            suit = s;
            rank = r;
        }

        int val() {
            if ("JQK".contains(rank))
                return 10;
            if ("A".equals(rank))
                return 11;
            return Integer.parseInt(rank);
        }

        public String toString() {
            return rank + "," + suit;
        }
    }

    static class Hand {
        List<Card> cards = new ArrayList<>();

        void add(Card c) {
            cards.add(c);
        }

        int bestValue() {
            int sum = 0, aces = 0;
            for (Card c : cards) {
                sum += c.val();
                if (c.rank.equals("A"))
                    aces++;
            }
            while (sum > 21 && aces > 0) {
                sum -= 10;
                aces--;
            }
            return sum;
        }

        boolean isBust() {
            return bestValue() > 21;
        }

        String toString(boolean showAll, boolean hideSecond) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < cards.size(); i++) {
                if (!showAll && hideSecond && i == 1)
                    sb.append("HIDDEN");
                else
                    sb.append(cards.get(i).toString());
                if (i < cards.size() - 1)
                    sb.append(";");
            }
            return sb.toString();
        }
    }
}