package server.ui;

import common.model.InventoryItem;
import common.model.User;
import server.core.ClientSessionManager;
import server.dao.ClientInventoryDao;
import server.dao.UserDao;
import server.service.InventoryMonitorService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Tab 1: Giam sat danh sach Dai ly va Tinh trang Ton kho thoi gian thuc (Novelty 1 Watch)
 */
public class ClientMonitorPanel extends JPanel {
    private final UserDao userDao;
    private final ClientInventoryDao inventoryDao;
    private final InventoryMonitorService inventoryService;

    private JTable retailerTable;
    private DefaultTableModel retailerModel;

    private JTable inventoryTable;
    private DefaultTableModel inventoryModel;
    private JLabel selectedClientLabel;
    private String currentSelectedClientId = null;

    public ClientMonitorPanel() {
        this.userDao = new UserDao();
        this.inventoryDao = new ClientInventoryDao();
        this.inventoryService = new InventoryMonitorService();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createLeftRetailerPanel(), createRightInventoryPanel());
        splitPane.setResizeWeight(0.42);
        splitPane.setDividerLocation(460);
        add(splitPane, BorderLayout.CENTER);

        refreshRetailers();
    }

    private JPanel createLeftRetailerPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Danh Sách Đại Lý Bán Lẻ (Retailers)"));

        String[] cols = {"Mã ĐL", "Tài khoản", "Họ Tên", "Số ĐT", "Trạng Thái"};
        retailerModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        retailerTable = new JTable(retailerModel);
        retailerTable.setRowHeight(26);
        retailerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Custom Renderer cho cot trang thai
        retailerTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                String status = value != null ? value.toString() : "";
                if (status.contains("ONLINE")) {
                    setForeground(new Color(40, 167, 69)); // Green
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    setForeground(Color.GRAY);
                    setFont(getFont().deriveFont(Font.PLAIN));
                }
                return c;
            }
        });

        retailerTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = retailerTable.getSelectedRow();
                if (row >= 0) {
                    currentSelectedClientId = (String) retailerModel.getValueAt(row, 0);
                    String name = (String) retailerModel.getValueAt(row, 2);
                    selectedClientLabel.setText("Đại lý đang chọn: " + name + " (" + currentSelectedClientId + ")");
                    refreshInventory();
                }
            }
        });

        panel.add(new JScrollPane(retailerTable), BorderLayout.CENTER);

        // Toolbar ben duoi
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JButton refreshBtn = new JButton("Làm Mới Danh Sách");
        refreshBtn.addActionListener(e -> refreshRetailers());
        toolbar.add(refreshBtn);

        JButton addBtn = new JButton("Thêm Đại Lý Mới");
        addBtn.addActionListener(e -> showAddRetailerDialog());
        toolbar.add(addBtn);

        panel.add(toolbar, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createRightInventoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Bảng Tồn Kho Đại Lý & Cảnh Báo Tự Động (Novelty 1)"));

        selectedClientLabel = new JLabel("Chọn một đại lý bên trái để xem bảng tồn kho chi tiết");
        selectedClientLabel.setFont(selectedClientLabel.getFont().deriveFont(Font.BOLD, 13f));
        selectedClientLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(selectedClientLabel, BorderLayout.NORTH);

        String[] cols = {"Mã SP", "Tên Sản Phẩm", "Tồn Kho", "Đã Bán", "Ngưỡng An Toàn", "Tình Trạng"};
        inventoryModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        inventoryTable = new JTable(inventoryModel);
        inventoryTable.setRowHeight(26);

        // Highlight canh bao hang sap het
        inventoryTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                String status = value != null ? value.toString() : "";
                if (status.contains("SẮP HẾT")) {
                    setForeground(new Color(220, 53, 69)); // Red danger
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    setForeground(new Color(40, 167, 69)); // Green ok
                    setFont(getFont().deriveFont(Font.PLAIN));
                }
                return c;
            }
        });

        panel.add(new JScrollPane(inventoryTable), BorderLayout.CENTER);

        // Toolbar action phia duoi
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));

        JButton pushAlertBtn = new JButton("Gửi Cảnh Báo Nhập Hàng (Push Alert)");
        pushAlertBtn.setBackground(new Color(220, 53, 69));
        pushAlertBtn.setForeground(Color.WHITE);
        pushAlertBtn.addActionListener(e -> {
            if (currentSelectedClientId == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một đại lý trước!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int sent = inventoryService.scanAndAlertClient(currentSelectedClientId);
            if (sent > 0) {
                JOptionPane.showMessageDialog(this,
                        String.format("Đã Server Push thành công %d cảnh báo tồn kho thấp xuống máy đại lý %s!", sent, currentSelectedClientId),
                        "Thành Công (Novelty 1)", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Kho của đại lý này hiện đều đang ở mức an toàn (hoặc đại lý đang offline)!",
                        "Thông Báo", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        actionPanel.add(pushAlertBtn);

        JButton scanAllBtn = new JButton("Quét Cảnh Báo Toàn Bộ Hệ Thống");
        scanAllBtn.addActionListener(e -> {
            int count = inventoryService.scanAllAndAlert();
            JOptionPane.showMessageDialog(this,
                    String.format("Đã quét và phát hiện %d cảnh báo tồn kho gửi tới các đại lý online!", count),
                    "Kết Quả Quét Toàn Hệ Thống", JOptionPane.INFORMATION_MESSAGE);
        });
        actionPanel.add(scanAllBtn);

        JButton reloadInvBtn = new JButton("Tải Lại Kho");
        reloadInvBtn.addActionListener(e -> refreshInventory());
        actionPanel.add(reloadInvBtn);

        panel.add(actionPanel, BorderLayout.SOUTH);
        return panel;
    }

    public void refreshRetailers() {
        int selectedRow = retailerTable.getSelectedRow();
        retailerModel.setRowCount(0);
        List<User> retailers = userDao.getAllRetailers();

        for (User u : retailers) {
            boolean isOnline = ClientSessionManager.isClientOnline(u.getUserId());
            String status = isOnline ? "● ONLINE" : "○ OFFLINE";
            retailerModel.addRow(new Object[]{
                    u.getUserId(), u.getUsername(), u.getFullName(), u.getPhone(), status
            });
        }

        if (selectedRow >= 0 && selectedRow < retailerModel.getRowCount()) {
            retailerTable.setRowSelectionInterval(selectedRow, selectedRow);
        } else if (retailerModel.getRowCount() > 0 && currentSelectedClientId == null) {
            retailerTable.setRowSelectionInterval(0, 0);
        }
    }

    public void refreshInventory() {
        inventoryModel.setRowCount(0);
        if (currentSelectedClientId == null) return;

        List<InventoryItem> items = inventoryDao.getInventoryByClient(currentSelectedClientId);
        for (InventoryItem item : items) {
            boolean isLow = item.getCurrentStock() <= item.getMinSafetyThreshold();
            String status = isLow ? "SẮP HẾT [CẢNH BÁO]" : "An toàn";
            inventoryModel.addRow(new Object[]{
                    item.getProductCode(),
                    item.getProductName(),
                    item.getCurrentStock(),
                    item.getSoldQuantity(),
                    item.getMinSafetyThreshold(),
                    status
            });
        }
    }

    private void showAddRetailerDialog() {
        JTextField idField = new JTextField("USR_" + System.currentTimeMillis() % 10000);
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField("123456");
        JTextField nameField = new JTextField();
        JTextField phoneField = new JTextField();

        Object[] message = {
                "Mã Đại Lý:", idField,
                "Tên Đăng Nhập:", userField,
                "Mật Khẩu:", passField,
                "Tên Đầy Đủ / Tên Shop:", nameField,
                "Số Điện Thoại:", phoneField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Thêm Đại Lý Bán Lẻ Mới", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String id = idField.getText().trim();
            String user = userField.getText().trim();
            String pass = new String(passField.getPassword()).trim();
            String name = nameField.getText().trim();
            String phone = phoneField.getText().trim();

            if (user.isEmpty() || pass.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            User newUser = new User(id, user, name, common.model.UserRole.CLIENT_RETAILER, phone);
            boolean ok = userDao.createUser(newUser, pass);
            if (ok) {
                JOptionPane.showMessageDialog(this, "Đã thêm đại lý mới thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                refreshRetailers();
            } else {
                JOptionPane.showMessageDialog(this, "Không thể thêm đại lý (Tên đăng nhập có thể đã tồn tại)!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
