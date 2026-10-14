package server.ui;

import common.model.ChatMessage;
import common.model.User;
import server.core.ClientSessionManager;
import server.dao.ChatDao;
import server.dao.UserDao;
import server.service.ServerChatService;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Tab 4: Kenh Chat Dam Phan 1-1 voi cac Dai Ly, Ho tro Gui/Nhan Tin Nhan va Xem Anh Mau San Pham
 */
public class ServerChatPanel extends JPanel {
    private final ChatDao chatDao;
    private final UserDao userDao;
    private final ServerChatService chatService;

    private JList<String> partnerList;
    private DefaultListModel<String> partnerListModel;

    private JEditorPane chatPane;
    private HTMLEditorKit htmlEditorKit;
    private HTMLDocument htmlDoc;

    private JTextField messageInputField;
    private JLabel currentPartnerLabel;
    private String currentSelectedPartnerId = null;

    public ServerChatPanel() {
        this.chatDao = new ChatDao();
        this.userDao = new UserDao();
        this.chatService = new ServerChatService();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createLeftPartnerListPanel(), createRightChatAreaPanel());
        splitPane.setResizeWeight(0.28);
        splitPane.setDividerLocation(280);
        add(splitPane, BorderLayout.CENTER);

        refreshPartners();
    }

    private JPanel createLeftPartnerListPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Đối Tác Trò Chuyện (Đại Lý)"));

        partnerListModel = new DefaultListModel<>();
        partnerList = new JList<>(partnerListModel);
        partnerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        partnerList.setFixedCellHeight(32);

        partnerList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = partnerList.getSelectedValue();
                if (selected != null) {
                    // Trích xuất ID: [USR_QUANG] Đại lý Quang
                    int startIdx = selected.indexOf('[');
                    int endIdx = selected.indexOf(']');
                    if (startIdx >= 0 && endIdx > startIdx) {
                        currentSelectedPartnerId = selected.substring(startIdx + 1, endIdx);
                        currentPartnerLabel.setText("Đang trao đổi với: " + selected);
                        loadConversation();
                    }
                }
            }
        });

        panel.add(new JScrollPane(partnerList), BorderLayout.CENTER);

        JButton refreshBtn = new JButton("Làm Mới Danh Sách");
        refreshBtn.addActionListener(e -> refreshPartners());
        panel.add(refreshBtn, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createRightChatAreaPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Nội Dung Hội Thoại Đàm Phán & Xem Ảnh Mẫu"));

        currentPartnerLabel = new JLabel("Chọn một đại lý bên trái để bắt đầu trò chuyện");
        currentPartnerLabel.setFont(currentPartnerLabel.getFont().deriveFont(Font.BOLD, 13f));
        currentPartnerLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(currentPartnerLabel, BorderLayout.NORTH);

        // Khung chat hien thi tin nhan HTML
        chatPane = new JEditorPane();
        chatPane.setEditable(false);
        htmlEditorKit = new HTMLEditorKit();
        chatPane.setEditorKit(htmlEditorKit);
        htmlDoc = (HTMLDocument) htmlEditorKit.createDefaultDocument();
        chatPane.setDocument(htmlDoc);

        panel.add(new JScrollPane(chatPane), BorderLayout.CENTER);

        // Input & Actions panel
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        messageInputField = new JTextField();
        messageInputField.setFont(messageInputField.getFont().deriveFont(13f));
        messageInputField.addActionListener(e -> sendMessageAction());
        inputPanel.add(messageInputField, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        JButton sendBtn = new JButton("Gửi Tin");
        sendBtn.setBackground(new Color(0, 123, 255));
        sendBtn.setForeground(Color.WHITE);
        sendBtn.addActionListener(e -> sendMessageAction());
        btnPanel.add(sendBtn);

        JButton viewImgBtn = new JButton("Xem Ảnh Đã Nhận");
        viewImgBtn.addActionListener(e -> showLatestImageDialog());
        btnPanel.add(viewImgBtn);

        inputPanel.add(btnPanel, BorderLayout.EAST);
        panel.add(inputPanel, BorderLayout.SOUTH);

        return panel;
    }

    public void refreshPartners() {
        partnerListModel.clear();
        List<User> retailers = userDao.getAllRetailers();

        for (User u : retailers) {
            boolean isOnline = ClientSessionManager.isClientOnline(u.getUserId());
            String statusDot = isOnline ? "●" : "○";
            partnerListModel.addElement(String.format("%s [%s] %s", statusDot, u.getUserId(), u.getFullName()));
        }

        if (!partnerListModel.isEmpty() && currentSelectedPartnerId == null) {
            partnerList.setSelectedIndex(0);
        }
    }

    public void loadConversation() {
        if (currentSelectedPartnerId == null) return;

        List<ChatMessage> messages = chatDao.getConversation("USR_ADMIN", currentSelectedPartnerId, 50);
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family:sans-serif; padding:10px;'>");

        for (ChatMessage m : messages) {
            boolean isAdmin = "USR_ADMIN".equalsIgnoreCase(m.getSenderId()) || "SERVER_ADMIN".equalsIgnoreCase(m.getSenderId());
            String align = isAdmin ? "right" : "left";
            String bg = isAdmin ? "#1e3a5f" : "#2d3748";
            String textColor = "#ffffff";
            String senderName = isAdmin ? "Hiếu (Server Admin)" : currentSelectedPartnerId;

            html.append("<div style='text-align:").append(align).append("; margin-bottom:10px;'>");
            html.append("<div style='display:inline-block; max-width:70%; text-align:left; background:").append(bg)
                    .append("; color:").append(textColor).append("; padding:8px 12px; border-radius:8px;'>");
            html.append("<div style='font-size:10px; color:#a0aec0;'><b>").append(senderName).append("</b> - ").append(m.getTimestamp()).append("</div>");

            if ("IMAGE".equalsIgnoreCase(m.getContentType())) {
                html.append("<div style='margin-top:5px;'>[HÌNH ẢNH MẪU SẢN PHẨM]</div>");
                html.append("<div style='font-size:11px; color:#cbd5e0;'><i>").append(m.getContent()).append("</i></div>");
            } else {
                html.append("<div style='margin-top:4px; font-size:13px;'>").append(escapeHtml(m.getContent())).append("</div>");
            }

            html.append("</div></div>");
        }

        html.append("</body></html>");
        chatPane.setText(html.toString());
        chatPane.setCaretPosition(chatPane.getDocument().getLength());
    }

    private void sendMessageAction() {
        if (currentSelectedPartnerId == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn đại lý nhận tin!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String text = messageInputField.getText().trim();
        if (text.isEmpty()) return;

        boolean ok = chatService.sendTextMessageFromAdmin(currentSelectedPartnerId, text);
        if (ok) {
            messageInputField.setText("");
            loadConversation();
        } else {
            JOptionPane.showMessageDialog(this, "Lỗi gửi tin nhắn!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showLatestImageDialog() {
        if (currentSelectedPartnerId == null) return;

        List<ChatMessage> messages = chatDao.getConversation("USR_ADMIN", currentSelectedPartnerId, 20);
        String lastImagePath = null;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("IMAGE".equalsIgnoreCase(messages.get(i).getContentType())) {
                lastImagePath = messages.get(i).getContent();
                break;
            }
        }

        if (lastImagePath == null) {
            JOptionPane.showMessageDialog(this, "Đại lý này chưa gửi hình ảnh mẫu sản phẩm nào!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        File imgFile = new File(lastImagePath);
        if (!imgFile.exists()) {
            JOptionPane.showMessageDialog(this, "File ảnh không tồn tại trên đĩa: " + lastImagePath, "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            BufferedImage img = ImageIO.read(imgFile);
            if (img != null) {
                // Thu nho neu anh qua to
                int maxW = 800, maxH = 600;
                int w = img.getWidth(), h = img.getHeight();
                if (w > maxW || h > maxH) {
                    double scale = Math.min((double) maxW / w, (double) maxH / h);
                    w = (int) (w * scale);
                    h = (int) (h * scale);
                }
                Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
                JLabel imgLabel = new JLabel(new ImageIcon(scaled));

                JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Ảnh Mẫu Sản Phẩm: " + imgFile.getName(), true);
                dialog.setLayout(new BorderLayout());
                dialog.add(new JScrollPane(imgLabel), BorderLayout.CENTER);
                dialog.pack();
                dialog.setLocationRelativeTo(this);
                dialog.setVisible(true);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi mở file ảnh: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br/>");
    }
}
