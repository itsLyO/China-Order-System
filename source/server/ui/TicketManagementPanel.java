package server.ui;

import common.model.OrderStatus;
import common.model.OrderTicket;
import common.model.PriceQuote;
import server.dao.OrderDao;
import server.service.OrderService;
import server.service.PricingService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Tab 2: Quan ly Ticket Order, Tinh toan Bao gia si bac thang, Sinh ma VietQR va Duyet coc
 */
public class TicketManagementPanel extends JPanel {
    private final OrderDao orderDao;
    private final OrderService orderService;
    private final PricingService pricingService;

    private JTable ticketTable;
    private DefaultTableModel ticketModel;
    private JComboBox<String> statusFilterCombo;

    // Form inputs
    private JLabel ticketIdLbl;
    private JLabel productLbl;
    private JLabel quantityLbl;
    private JLabel clientNoteLbl;
    private JTextField rawPriceCnyField;
    private JTextField exchangeRateField;
    private JTextField domesticShippingCnyField;
    private JTextField intlShippingVndField;

    // Result labels
    private JLabel effectivePriceCnyLbl;
    private JLabel totalVndLbl;
    private JLabel depositRequiredLbl;
    private JTextField qrLinkField;

    private OrderTicket currentTicket = null;
    private PriceQuote currentCalculatedQuote = null;

    public TicketManagementPanel() {
        this.orderDao = new OrderDao();
        this.orderService = new OrderService();
        this.pricingService = new PricingService();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, createTopTicketTablePanel(), createBottomPricingPanel());
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerLocation(320);
        add(splitPane, BorderLayout.CENTER);

        refreshTickets();
    }

    private JPanel createTopTicketTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Danh Sách Yêu Cầu Đặt Hàng (Order Tickets)"));

        // Filter Header
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterPanel.add(new JLabel("Lọc theo trạng thái:"));
        statusFilterCombo = new JComboBox<>(new String[]{
                "TẤT CẢ",
                OrderStatus.PENDING_QUOTE.name(),
                OrderStatus.QUOTED.name(),
                OrderStatus.DEPOSITED.name(),
                OrderStatus.ORDERED_CN.name(),
                OrderStatus.WAREHOUSE_BORDER_CN.name(),
                OrderStatus.COMPLETED.name()
        });
        statusFilterCombo.addActionListener(e -> refreshTickets());
        filterPanel.add(statusFilterCombo);

        JButton refreshBtn = new JButton("Làm Mới Bảng");
        refreshBtn.addActionListener(e -> refreshTickets());
        filterPanel.add(refreshBtn);

        panel.add(filterPanel, BorderLayout.NORTH);

        String[] cols = {"Mã Vé", "Đại Lý", "Tên Sản Phẩm", "SL", "Màu Sắc", "Kích Cỡ", "Trạng Thái", "Ngày Tạo"};
        ticketModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        ticketTable = new JTable(ticketModel);
        ticketTable.setRowHeight(25);
        ticketTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        ticketTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                String st = value != null ? value.toString() : "";
                if (OrderStatus.PENDING_QUOTE.name().equals(st)) {
                    setForeground(new Color(255, 152, 0)); // Orange
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if (OrderStatus.QUOTED.name().equals(st)) {
                    setForeground(new Color(33, 150, 243)); // Blue
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if (OrderStatus.DEPOSITED.name().equals(st) || OrderStatus.COMPLETED.name().equals(st)) {
                    setForeground(new Color(76, 175, 80)); // Green
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    setForeground(Color.LIGHT_GRAY);
                    setFont(getFont().deriveFont(Font.PLAIN));
                }
                return c;
            }
        });

        ticketTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = ticketTable.getSelectedRow();
                if (row >= 0) {
                    String ticketId = (String) ticketModel.getValueAt(row, 0);
                    loadTicketDetail(ticketId);
                }
            }
        });

        panel.add(new JScrollPane(ticketTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBottomPricingPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Công Cụ Tính Giá Bậc Thang, Phát Hành Báo Giá & Quản Lý Cọc"));

        JPanel formGrid = new JPanel(new GridLayout(4, 4, 10, 8));

        // Hang 1
        formGrid.add(new JLabel("Mã Vé:"));
        ticketIdLbl = new JLabel("(Chưa chọn)");
        ticketIdLbl.setFont(ticketIdLbl.getFont().deriveFont(Font.BOLD));
        formGrid.add(ticketIdLbl);

        formGrid.add(new JLabel("Đơn Giá Xưởng Gốc (CNY):"));
        rawPriceCnyField = new JTextField("45.0");
        formGrid.add(rawPriceCnyField);

        // Hang 2
        formGrid.add(new JLabel("Sản Phẩm & Số Lượng:"));
        productLbl = new JLabel("-");
        formGrid.add(productLbl);

        formGrid.add(new JLabel("Tỷ Giá (VND / CNY):"));
        exchangeRateField = new JTextField("3650.0");
        formGrid.add(exchangeRateField);

        // Hang 3
        formGrid.add(new JLabel("Ghi Chú Đại Lý:"));
        clientNoteLbl = new JLabel("-");
        formGrid.add(clientNoteLbl);

        formGrid.add(new JLabel("Ship Nội Địa TQ (CNY):"));
        domesticShippingCnyField = new JTextField("50.0");
        formGrid.add(domesticShippingCnyField);

        // Hang 4
        formGrid.add(new JLabel("Đơn Giá CNY Sau Chiết Khấu:"));
        effectivePriceCnyLbl = new JLabel("-");
        effectivePriceCnyLbl.setForeground(new Color(33, 150, 243));
        formGrid.add(effectivePriceCnyLbl);

        formGrid.add(new JLabel("Ship Quốc Tế TQ-VN (VND):"));
        intlShippingVndField = new JTextField("1200000");
        formGrid.add(intlShippingVndField);

        panel.add(formGrid, BorderLayout.NORTH);

        // Result Summary & Action Panel
        JPanel centerResultPanel = new JPanel(new GridLayout(2, 2, 10, 6));
        centerResultPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Kết Quả Báo Giá & VietQR"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        totalVndLbl = new JLabel("Tổng Tiền Ước Tính: 0 VND");
        totalVndLbl.setFont(totalVndLbl.getFont().deriveFont(Font.BOLD, 14f));
        totalVndLbl.setForeground(new Color(255, 193, 7));
        centerResultPanel.add(totalVndLbl);

        depositRequiredLbl = new JLabel("Tiền Cọc Yêu Cầu (30%): 0 VND");
        depositRequiredLbl.setFont(depositRequiredLbl.getFont().deriveFont(Font.BOLD, 14f));
        depositRequiredLbl.setForeground(new Color(76, 175, 80));
        centerResultPanel.add(depositRequiredLbl);

        centerResultPanel.add(new JLabel("Link Mã VietQR:"));
        qrLinkField = new JTextField();
        qrLinkField.setEditable(false);
        centerResultPanel.add(qrLinkField);

        panel.add(centerResultPanel, BorderLayout.CENTER);

        // Button Toolbar
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        JButton calcBtn = new JButton("Tự Động Tính Giá (Auto Calc)");
        calcBtn.addActionListener(e -> calculateQuoteAction());
        btnBar.add(calcBtn);

        JButton issueBtn = new JButton("Phát Hành Báo Giá & Push Về Đại Lý");
        issueBtn.setBackground(new Color(0, 123, 255));
        issueBtn.setForeground(Color.WHITE);
        issueBtn.addActionListener(e -> issueQuoteAction());
        btnBar.add(issueBtn);

        JButton confirmDepositBtn = new JButton("Xác Nhận Đã Nhận Cọc (Deposit)");
        confirmDepositBtn.setBackground(new Color(40, 167, 69));
        confirmDepositBtn.setForeground(Color.WHITE);
        confirmDepositBtn.addActionListener(e -> confirmDepositAction());
        btnBar.add(confirmDepositBtn);

        panel.add(btnBar, BorderLayout.SOUTH);
        return panel;
    }

    private void loadTicketDetail(String ticketId) {
        currentTicket = orderDao.getTicketById(ticketId);
        if (currentTicket == null) return;

        ticketIdLbl.setText(currentTicket.getTicketId());
        productLbl.setText(currentTicket.getProductName() + " (SL: " + currentTicket.getQuantity() + ")");
        clientNoteLbl.setText(currentTicket.getClientNote() != null ? currentTicket.getClientNote() : "Không có");

        // Neu da co bao gia trong DB thi tai len form
        PriceQuote existingQuote = orderDao.getPriceQuoteByTicketId(ticketId);
        if (existingQuote != null) {
            rawPriceCnyField.setText(String.format("%.2f", existingQuote.getUnitPriceCny()));
            exchangeRateField.setText(String.format("%.0f", existingQuote.getExchangeRate()));
            domesticShippingCnyField.setText(String.format("%.1f", existingQuote.getDomesticShippingCny()));
            intlShippingVndField.setText(String.format("%.0f", existingQuote.getIntlShippingVnd()));
            effectivePriceCnyLbl.setText(String.format("%.2f CNY", existingQuote.getUnitPriceCny()));
            totalVndLbl.setText(String.format("Tổng Tiền: %,.0f VND", existingQuote.getTotalEstimatedVnd()));
            depositRequiredLbl.setText(String.format("Tiền Cọc (30%%): %,.0f VND", existingQuote.getDepositRequiredVnd()));
            qrLinkField.setText(existingQuote.getQrPayload());
            currentCalculatedQuote = existingQuote;
        } else {
            // Tinh gia mac dinh theo so luong cua ticket
            calculateQuoteAction();
        }
    }

    private void calculateQuoteAction() {
        if (currentTicket == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một Ticket trước!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            double rawPrice = Double.parseDouble(rawPriceCnyField.getText().trim());
            double exchangeRate = Double.parseDouble(exchangeRateField.getText().trim());
            double domestic = Double.parseDouble(domesticShippingCnyField.getText().trim());
            double intl = Double.parseDouble(intlShippingVndField.getText().trim());

            currentCalculatedQuote = pricingService.calculatePriceQuote(
                    currentTicket.getTicketId(), rawPrice, currentTicket.getQuantity(),
                    exchangeRate, domestic, intl
            );

            double discountRate = pricingService.getDiscountRate(currentTicket.getQuantity());
            effectivePriceCnyLbl.setText(String.format("%.2f CNY (Giảm %.0f%%)", currentCalculatedQuote.getUnitPriceCny(), discountRate * 100));
            totalVndLbl.setText(String.format("Tổng Tiền: %,.0f VND", currentCalculatedQuote.getTotalEstimatedVnd()));
            depositRequiredLbl.setText(String.format("Tiền Cọc (30%%): %,.0f VND", currentCalculatedQuote.getDepositRequiredVnd()));
            qrLinkField.setText(currentCalculatedQuote.getQrPayload());

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đúng định dạng số cho giá và cước!", "Lỗi định dạng", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void issueQuoteAction() {
        if (currentTicket == null || currentCalculatedQuote == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn Ticket và tính giá trước khi phát hành!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean ok = orderService.issuePriceQuote(currentCalculatedQuote);
        if (ok) {
            JOptionPane.showMessageDialog(this,
                    String.format("Đã phát hành báo giá và Server Push mã VietQR thành công tới đại lý [%s]!", currentTicket.getClientId()),
                    "Thành Công", JOptionPane.INFORMATION_MESSAGE);
            refreshTickets();
        } else {
            JOptionPane.showMessageDialog(this, "Không thể lưu báo giá vào CSDL!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void confirmDepositAction() {
        if (currentTicket == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn Ticket cần duyệt cọc!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean ok = orderService.updateTrackingStage(
                currentTicket.getTicketId(), OrderStatus.DEPOSITED,
                "Hà Nội (Server)", "Admin Hiếu đã xác nhận nhận cọc thành công", "Hiếu - Server"
        );
        if (ok) {
            JOptionPane.showMessageDialog(this, "Đã duyệt cọc thành công và Server Push thông báo về máy đại lý!", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
            refreshTickets();
        } else {
            JOptionPane.showMessageDialog(this, "Lỗi duyệt cọc đơn hàng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refreshTickets() {
        int selectedRow = ticketTable.getSelectedRow();
        String filter = (String) statusFilterCombo.getSelectedItem();
        ticketModel.setRowCount(0);

        List<OrderTicket> tickets = orderDao.getAllTickets();
        for (OrderTicket t : tickets) {
            if ("TẤT CẢ".equals(filter) || (t.getStatus() != null && t.getStatus().name().equals(filter))) {
                ticketModel.addRow(new Object[]{
                        t.getTicketId(),
                        t.getClientId(),
                        t.getProductName(),
                        t.getQuantity(),
                        t.getColor() != null ? t.getColor() : "-",
                        t.getSize() != null ? t.getSize() : "-",
                        t.getStatus() != null ? t.getStatus().name() : "-",
                        t.getCreatedAt() != null ? t.getCreatedAt() : "-"
                });
            }
        }

        if (selectedRow >= 0 && selectedRow < ticketModel.getRowCount()) {
            ticketTable.setRowSelectionInterval(selectedRow, selectedRow);
        } else if (ticketModel.getRowCount() > 0 && currentTicket == null) {
            ticketTable.setRowSelectionInterval(0, 0);
        }
    }
}
