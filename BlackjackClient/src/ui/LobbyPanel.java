import javax.swing.*;
import java.awt.*;

/**
 * 大廳畫面
 */
public class LobbyPanel extends JPanel {
    private final JButton pveButton;
    private final JButton createRoomButton;
    private final JTextField roomIdField;
    private final JButton joinRoomButton;

    public LobbyPanel() {
        setLayout(new GridLayout(3, 1, 20, 20));
        setBorder(BorderFactory.createEmptyBorder(80, 200, 80, 200));

        pveButton = new JButton("單人練習 (PVE)");
        createRoomButton = new JButton("創建房間 (多人亂鬥)");

        JPanel joinPanel = new JPanel();
        roomIdField = new JTextField(6);
        joinRoomButton = new JButton("加入房間");
        joinPanel.add(new JLabel("房號:"));
        joinPanel.add(roomIdField);
        joinPanel.add(joinRoomButton);

        add(pveButton);
        add(createRoomButton);
        add(joinPanel);
    }

    public JButton getPveButton() {
        return pveButton;
    }

    public JButton getCreateRoomButton() {
        return createRoomButton;
    }

    public JButton getJoinRoomButton() {
        return joinRoomButton;
    }

    public String getRoomId() {
        return roomIdField.getText().trim();
    }
}
