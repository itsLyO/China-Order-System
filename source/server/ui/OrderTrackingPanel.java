package server.ui;

import common.model.OrderStatus;
import common.model.OrderStatusUpdate;
import common.model.OrderTicket;
import server.dao.OrderDao;
import server.service.OrderService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Tab 3: Quan ly Tien Trinh 7 Chang Logistics TQ-VN va Server Push thoi gian thuc ve Client
 */
public class OrderTrackingPanel extends JPanel {
    private final OrderDao orderDao;
    private final OrderService orderService;

    private JTable ordersTable;
    private DefaultTableModel ordersModel;

    private JTable timelineTable;
    private DefaultTableModel timelineModel;

    private JLabel selectedTicketLbl;
    private JComboBox<OrderStatus> stageComboBox;
    private JTextField locationField;
    private JTextField noteField;

    private OrderTicket currentTicket = null;

    public OrderTrackingPanel() {
        this.orderDao = new OrderDao();
        this.orderService = new OrderService();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, createTopOrdersPanel(), createBottomTimelinePanel());
        splitPane.setResizeWeight(0.45);
        splitPane.setDividerLocation(300);
        add(splitPane, BorderLayout.CENTER);

        refreshOrders();
    }

    private JPanel createTopOrdersPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Danh Sách Đơn Hàng Đang Trong Luồng Vận Chuyển"));

        String[] cols = {"Mã Đơn", "Đại Lý", "Sản Phẩm", "Số Lượng", "Trạng Thái Hiện Tại", "Ngày Tạo"};
        ordersModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        ordersTable = new JTable(ordersModel);
        ordersTable.setRowHeight(25);
        ordersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        ordersTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                String st = value != null ? value.toString() : "";
                if (OrderStatus.COMPLETED.name().equals(st)) {
                    setForeground(new Color(40, 167, 69)); // Green
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if (OrderStatus.CUSTOMS_CLEARANCE.name().equals(st) || OrderStatus.WAREHOUSE_BORDER_CN.name().equals(st)) {
                    setForeground(new Color(255, 152, 0)); // Orange
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    setForeground(new Color(33, 150, 243)); // Blue
                }
                return c;
            }
        });

        ordersTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = ordersTable.getSelectedRow();
                if (row >= 0) {
                    String ticketId = (String) ordersModel.getValueAt(row, 0);
                    selectTicket(ticketId);
                }
            }
        });

        panel.add(new JScrollPane(ordersTable), BorderLayout.CENTER);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JButton refreshBtn = new JButton("Làm Mới Danh Sách");
        refreshBtn.addActionListener(e -> refreshOrders());
        toolbar.add(refreshBtn);
        panel.add(toolbar, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createBottomTimelinePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Cập Nhật Chặng Logistics & Lịch Sử Hành Trình"));

        // Controller Form
        JPanel controlPanel = new JPanel(new GridLayout(3, 2, 10, 8));
        controlPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Cập Nhật Chặng Vận Chuyển Mới (Server Push)"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        selectedTicketLbl = new JLabel("Đơn hàng đang chọn: (Chưa chọn)");
        selectedTicketLbl.setFont(selectedTicketLbl.getFont().deriveFont(Font.BOLD, 13f));
        controlPanel.add(selectedTicketLbl);

        JPanel comboRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        comboRow.add(new JLabel("Chọn chặng tiếp theo:"));
        stageComboBox = new JComboBox<>(new OrderStatus[]{
                OrderStatus.ORDERED_CN,
                OrderStatus.DOMESTIC_SHIPPING_CN,
                OrderStatus.WAREHOUSE_BORDER_CN,
                OrderStatus.CUSTOMS_CLEARANCE,
                OrderStatus.SHIPPING_VN,
                OrderStatus.COMPLETED,
                OrderStatus.CANCELLED
        });
        comboRow.add(stageComboBox);
        controlPanel.add(comboRow);

        JPanel locRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        locRow.add(new JLabel("Vị trí / Địa điểm:"));
        locationField = new JTextField("Kho Bằng Tường (Biên giới TQ)", 25);
        locRow.add(locationField);
        controlPanel.add(locRow);

        JPanel noteRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        noteRow.add(new JLabel("Ghi chú tiến độ:"));
        noteField = new JTextField("Hàng đã về kho biên giới, chuẩn bị thông quan", 28);
        noteRow.add(noteField);
        controlPanel.add(noteRow);

        JButton pushStageBtn = new JButton("Cập Nhật Chặng & Server Push Tức Thì");
        pushStageBtn.setBackground(new Color(255, 87, 34)); // Orange red
        pushStageBtn.setForeground(Color.WHITE);
        pushStageBtn.setFont(pushStageBtn.getFont().deriveFont(Font.BOLD));
        pushStageBtn.addActionListener(e -> updateStageAction());
        controlPanel.add(pushStageBtn);

        panel.add(controlPanel, BorderLayout.NORTH);

        // Timeline History Table
        String[] timelineCols = {"Thời Gian", "Trạng Thái Chặng", "Địa Điểm", "Ghi Chú", "Người Cập Nhật"};
        timelineModel = new DefaultTableModel(timelineCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        timelineTable = new JTable(timelineModel);
        timelineTable.setRowHeight(24);

        JPanel timelineContainer = new JPanel(new BorderLayout(5, 5));
        timelineContainer.setBorder(BorderFactory.createTitledBorder("Nhật Ký Hành Trình Chi Tiết (Logistics Tracking Logs)"));
        timelineContainer.add(new JScrollPane(timelineTable), BorderLayout.CENTER);

        panel.add(timelineContainer, BorderLayout.CENTER);
        return panel;
    }

    private void selectTicket(String ticketId) {
        currentTicket = orderDao.getTicketById(ticketId);
        if (currentTicket == null) return;

        selectedTicketLbl.setText("Đơn hàng: " + currentTicket.getTicketId() + " - " + currentTicket.getProductName() + " (" + currentTicket.getClientId() + ")");
        loadTimelineLogs(ticketId);
    }

    private void updateStageAction() {
        if (currentTicket == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một đơn hàng trước!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        OrderStatus newStatus = (OrderStatus) stageComboBox.getSelectedItem();
        String location = locationField.getText().trim();
        String note = noteField.getText().trim();

        if (location.isEmpty()) {
            location = "Việt Nam";
        }

        boolean ok = orderService.updateTrackingStage(currentTicket.getTicketId(), newStatus, location, note, "Hiếu - Server");
        if (ok) {
            JOptionPane.showMessageDialog(this,
                    String.format("Đã chuyển đơn sang chặng [%s] và Server Push cập nhật tới máy đại lý [%s]!",
                            newStatus.name(), currentTicket.getClientId()),
                    "Thành Công (Server Push)", JOptionPane.INFORMATION_MESSAGE);
            refreshOrders();
            selectTicket(currentTicket.getTicketId());
        } else {
            JOptionPane.showMessageDialog(this, "Không thể cập nhật chặng vận chuyển!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadTimelineLogs(String ticketId) {
        timelineModel.setRowCount(0);
        List<OrderStatusUpdate> logs = orderDao.getTrackingLogs(ticketId);
        for (OrderStatusUpdate log : logs) {
            timelineModel.addRow(new Object[]{
                    log.getTimestamp(),
                    log.getNewStatus() != null ? log.getNewStatus().name() : "-",
                    log.getLocation() != null ? log.getLocation() : "-",
                    log.getNote() != null ? log.getNote() : "-",
                    "Hiếu - Server"
            });
        }
    }

    public void refreshOrders() {
        int selectedRow = ordersTable.getSelectedRow();
        ordersModel.setRowCount(0);

        List<OrderTicket> tickets = orderDao.getAllTickets();
        for (OrderTicket t : tickets) {
            // Hien thi tat ca hoac uu tien cac don tu QUOTED tro di
            ordersModel.addRow(new Object[]{
                    t.getTicketId(),
                    t.getClientId(),
                    t.getProductName(),
                    t.getQuantity(),
                    t.getStatus() != null ? t.getStatus().name() : "-",
                    t.getCreatedAt() != null ? t.getCreatedAt() : "-"
            });
        }

        if (selectedRow >= 0 && selectedRow < ordersModel.getRowCount()) {
            ordersTable.setRowSelectionInterval(selectedRow, selectedRow);
        } else if (ordersModel.getRowCount() > 0 && currentTicket == null) {
            ordersTable.setRowSelectionInterval(0, 0);
        }
    }
}
