package server.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Tab 5: Nhat ky Mang va Console giam sat su kien Server thoi gian thuc
 */
public class ServerLogPanel extends JPanel {
    private static ServerLogPanel instance;

    private JTable logTable;
    private DefaultTableModel logModel;
    private JComboBox<String> levelFilterCombo;

    public ServerLogPanel() {
        instance = this;

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolbar.add(new JLabel("Lọc theo cấp độ:"));

        levelFilterCombo = new JComboBox<>(new String[]{"TẤT CẢ", "INFO", "WARN", "ERROR"});
        toolbar.add(levelFilterCombo);

        JButton clearBtn = new JButton("Xóa Log");
        clearBtn.addActionListener(e -> logModel.setRowCount(0));
        toolbar.add(clearBtn);

        JButton copyBtn = new JButton("Sao Chép Log");
        copyBtn.addActionListener(e -> copyLogsToClipboard());
        toolbar.add(copyBtn);

        add(toolbar, BorderLayout.NORTH);

        // Log Table
        String[] cols = {"Thời Gian", "Cấp Độ", "Module / Nguồn", "Nội Dung Sự Kiện"};
        logModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        logTable = new JTable(logModel);
        logTable.setRowHeight(24);

        logTable.getColumnModel().getColumn(0).setPreferredWidth(140);
        logTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        logTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        logTable.getColumnModel().getColumn(3).setPreferredWidth(600);

        // Custom renderer cho cot cap do
        logTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                String lvl = value != null ? value.toString() : "";
                if ("ERROR".equalsIgnoreCase(lvl)) {
                    setForeground(new Color(220, 53, 69)); // Red
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if ("WARN".equalsIgnoreCase(lvl)) {
                    setForeground(new Color(255, 152, 0)); // Orange
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    setForeground(new Color(40, 167, 69)); // Green
                    setFont(getFont().deriveFont(Font.PLAIN));
                }
                return c;
            }
        });

        add(new JScrollPane(logTable), BorderLayout.CENTER);

        // Log khoi tao ban dau
        appendLog("INFO", "ServerLogPanel", "Giao dien giam sat log mang Server da san sang.");
    }

    public static ServerLogPanel getInstance() {
        return instance;
    }

    public synchronized void appendLog(String level, String tag, String message) {
        SwingUtilities.invokeLater(() -> {
            String time = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
            logModel.addRow(new Object[]{time, level, tag, message});
            if (logTable.getRowCount() > 0) {
                logTable.scrollRectToVisible(logTable.getCellRect(logTable.getRowCount() - 1, 0, true));
            }
        });
    }

    private void copyLogsToClipboard() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < logModel.getRowCount(); i++) {
            sb.append(String.format("[%s] [%s] [%s] %s\n",
                    logModel.getValueAt(i, 0),
                    logModel.getValueAt(i, 1),
                    logModel.getValueAt(i, 2),
                    logModel.getValueAt(i, 3)
            ));
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
        JOptionPane.showMessageDialog(this, "Đã sao chép toàn bộ nhật ký vào Clipboard!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }
}
