package server.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import common.util.LoggerUtil;
import server.core.ClientSessionManager;
import server.core.OrderServer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Cua so dieu hanh trung tam Server Control Center (Admin: Hieu)
 * Tich hop giao dien FlatLaf, 5 tab quan tri chuyen sau, tu dong khoi dong Socket Server 8888
 */
public class ServerMainFrame extends JFrame {
    private final OrderServer orderServer;
    private final int serverPort = OrderServer.DEFAULT_PORT;

    private JLabel statusBadge;
    private JLabel onlineCountLbl;
    private JButton toggleServerBtn;
    private boolean isDarkMode = true;

    private ClientMonitorPanel clientMonitorPanel;
    private TicketManagementPanel ticketManagementPanel;
    private OrderTrackingPanel orderTrackingPanel;
    private ServerChatPanel serverChatPanel;
    private ServerLogPanel serverLogPanel;

    private Timer autoRefreshTimer;

    public ServerMainFrame() {
        this.orderServer = new OrderServer();

        setTitle("CHINA ORDER SYSTEM - SERVER CONTROL CENTER (Admin: Hiếu)");
        setSize(1200, 800);
        setMinimumSize(new Dimension(1000, 680));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
        initSocketServer();
        initAutoRefresh();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (autoRefreshTimer != null) autoRefreshTimer.stop();
                orderServer.stop();
            }
        });
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // 1. Top Control Header Bar
        JPanel headerPanel = new JPanel(new BorderLayout(10, 5));
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(60, 63, 65)),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        // Tieu de & Thong tin Admin
        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 2, 2));
        JLabel titleLbl = new JLabel("CHINA ORDER & LOGISTICS - HỆ THỐNG QUẢN TRỊ TRUNG TÂM");
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD, 16f));
        titlePanel.add(titleLbl);

        JLabel adminLbl = new JLabel("Phụ trách máy chủ: Hiếu (Server Lead) | Chuyên Order xưởng & Logistics TQ-VN");
        adminLbl.setForeground(new Color(160, 174, 192));
        adminLbl.setFont(adminLbl.getFont().deriveFont(12f));
        titlePanel.add(adminLbl);
        headerPanel.add(titlePanel, BorderLayout.WEST);

        // Control strip ben phai
        JPanel controlStrip = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 5));

        statusBadge = new JLabel("● ĐANG KHỞI ĐỘNG");
        statusBadge.setFont(statusBadge.getFont().deriveFont(Font.BOLD, 13f));
        statusBadge.setForeground(new Color(255, 193, 7));
        controlStrip.add(statusBadge);

        JLabel portLbl = new JLabel("Cổng: " + serverPort);
        portLbl.setFont(portLbl.getFont().deriveFont(Font.BOLD, 13f));
        controlStrip.add(portLbl);

        onlineCountLbl = new JLabel("Đại lý online: 0");
        onlineCountLbl.setFont(onlineCountLbl.getFont().deriveFont(Font.BOLD, 13f));
        onlineCountLbl.setForeground(new Color(76, 175, 80));
        controlStrip.add(onlineCountLbl);

        toggleServerBtn = new JButton("Dừng Server");
        toggleServerBtn.setBackground(new Color(220, 53, 69));
        toggleServerBtn.setForeground(Color.WHITE);
        toggleServerBtn.addActionListener(e -> toggleServerAction());
        controlStrip.add(toggleServerBtn);

        JButton themeBtn = new JButton("🌓 Giao Diện");
        themeBtn.addActionListener(e -> toggleTheme());
        controlStrip.add(themeBtn);

        headerPanel.add(controlStrip, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // 2. Central TabbedPane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(tabbedPane.getFont().deriveFont(Font.BOLD, 13f));

        clientMonitorPanel = new ClientMonitorPanel();
        ticketManagementPanel = new TicketManagementPanel();
        orderTrackingPanel = new OrderTrackingPanel();
        serverChatPanel = new ServerChatPanel();
        serverLogPanel = new ServerLogPanel();

        tabbedPane.addTab("👥 Đại Lý & Tồn Kho (Novelty 1)", clientMonitorPanel);
        tabbedPane.addTab("📋 Duyệt Ticket & Báo Giá", ticketManagementPanel);
        tabbedPane.addTab("🚚 Quản Lý 7 Chặng Logistics", orderTrackingPanel);
        tabbedPane.addTab("💬 Kênh Chat 1-1 & Ảnh Mẫu", serverChatPanel);
        tabbedPane.addTab("📜 Nhật Ký Mạng Real-Time", serverLogPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // 3. Bottom Status Bar
        JPanel bottomBar = new JPanel(new BorderLayout(10, 2));
        bottomBar.setBorder(BorderFactory.createEmptyBorder(4, 15, 4, 15));
        JLabel dbStatusLbl = new JLabel("SQLite: data/server_storage.db (Connected) | ThreadPool: Active");
        dbStatusLbl.setFont(dbStatusLbl.getFont().deriveFont(11f));
        dbStatusLbl.setForeground(Color.GRAY);
        bottomBar.add(dbStatusLbl, BorderLayout.WEST);

        JLabel copyrightLbl = new JLabel("Lập Trình Mạng 2026-2027 | Server Node by Hiếu");
        copyrightLbl.setFont(copyrightLbl.getFont().deriveFont(11f));
        copyrightLbl.setForeground(Color.GRAY);
        bottomBar.add(copyrightLbl, BorderLayout.EAST);
        add(bottomBar, BorderLayout.SOUTH);
    }

    private void initSocketServer() {
        orderServer.startAsync(serverPort);
        updateServerStatusBadge(true);
        if (serverLogPanel != null) {
            serverLogPanel.appendLog("INFO", "OrderServer", "Socket Server da san sang lang nghe ket noi tren port " + serverPort);
        }
    }

    private void toggleServerAction() {
        if (orderServer.isRunning()) {
            orderServer.stop();
            updateServerStatusBadge(false);
            if (serverLogPanel != null) {
                serverLogPanel.appendLog("WARN", "OrderServer", "Admin Hieu da dung Server.");
            }
        } else {
            orderServer.startAsync(serverPort);
            updateServerStatusBadge(true);
            if (serverLogPanel != null) {
                serverLogPanel.appendLog("INFO", "OrderServer", "Admin Hieu da khoi dong lai Server tai port " + serverPort);
            }
        }
    }

    private void updateServerStatusBadge(boolean running) {
        if (running) {
            statusBadge.setText("● ONLINE");
            statusBadge.setForeground(new Color(40, 167, 69)); // Green
            toggleServerBtn.setText("Dừng Server");
            toggleServerBtn.setBackground(new Color(220, 53, 69)); // Red
        } else {
            statusBadge.setText("○ ĐÃ DỪNG");
            statusBadge.setForeground(new Color(220, 53, 69)); // Red
            toggleServerBtn.setText("Bật Server");
            toggleServerBtn.setBackground(new Color(40, 167, 69)); // Green
            onlineCountLbl.setText("Đại lý online: 0");
        }
    }

    private void toggleTheme() {
        try {
            if (isDarkMode) {
                UIManager.setLookAndFeel(new FlatLightLaf());
                isDarkMode = false;
            } else {
                UIManager.setLookAndFeel(new FlatDarkLaf());
                isDarkMode = true;
            }
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ex) {
            LoggerUtil.error("ServerMainFrame", "Loi chuyen doi Look and Feel", ex);
        }
    }

    private void initAutoRefresh() {
        // Tu dong dong bo so lieu moi 3 giay
        autoRefreshTimer = new Timer(3000, e -> {
            if (orderServer.isRunning()) {
                int online = ClientSessionManager.getOnlineCount();
                onlineCountLbl.setText("Đại lý online: " + online);

                if (clientMonitorPanel != null) clientMonitorPanel.refreshRetailers();
                if (serverChatPanel != null) serverChatPanel.refreshPartners();
            }
        });
        autoRefreshTimer.start();
    }

    public static void main(String[] args) {
        // Thiet lap FlatLaf Dark theme mac dinh
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            LoggerUtil.warn("ServerMainFrame", "Khong the nap FlatDarkLaf, su dung giao dien mac dinh.");
        }

        SwingUtilities.invokeLater(() -> {
            ServerMainFrame frame = new ServerMainFrame();
            frame.setVisible(true);
        });
    }
}
