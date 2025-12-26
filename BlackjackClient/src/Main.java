import javax.swing.*;

/**
 * 21點客戶端程式進入點
 */
public class Main {
    public static void main(String[] args) {
        // 設定 Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // 使用預設
        }
        
        SwingUtilities.invokeLater(() -> {
            new BlackjackClient().setVisible(true);
        });
    }
}
