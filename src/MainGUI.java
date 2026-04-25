import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class MainGUI extends JFrame {
    private DatabaseManipulation dm;
    private int currentUserId = -1;
    private int permission = 0;

    private JPanel cardPanel;
    private CardLayout cardLayout;
    private DefaultTableModel ticketModel;
    private DefaultTableModel orderModel;

    public MainGUI() {
        dm = new DatabaseManipulation();
        dm.openDatasource();

        setTitle("航空订票系统 - GUI 版");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 设置圆角美化
        UIManager.put("Button.arc", 15);
        UIManager.put("Component.arc", 15);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        initLoginPanel();
        add(cardPanel);
        setVisible(true);
    }

    private void initLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JTextField phoneField = new JTextField("13800138000", 15);
        JPasswordField passField = new JPasswordField(15);
        JButton loginBtn = new JButton(" 登 录 ");
        JButton visitorBtn = new JButton("游客访问");

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("手机号:"), gbc);
        gbc.gridx = 1; panel.add(phoneField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("密  码:"), gbc);
        gbc.gridx = 1; panel.add(passField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; panel.add(loginBtn, gbc);
        gbc.gridy = 3; panel.add(visitorBtn, gbc);

        loginBtn.addActionListener(e -> {
            String phone = phoneField.getText();
            String pwd = new String(passField.getPassword());
            if (dm.getPassword(phone).equals(pwd)) {
                currentUserId = dm.getId(phone);
                permission = dm.getPermission(phone);
                initDashboard();
                cardLayout.show(cardPanel, "dashboard");
            } else { JOptionPane.showMessageDialog(this, "登录失败"); }
        });

        visitorBtn.addActionListener(e -> {
            permission = 0; initDashboard(); cardLayout.show(cardPanel, "dashboard");
        });

        cardPanel.add(panel, "login");
    }

    private void initDashboard() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(" 航班查询 ", createSearchTab());
        if (permission >= 1) tabs.addTab(" 我的订单 ", createOrderTab());
        if (permission >= 2) tabs.addTab(" 管理员后台 ", createAdminTab());

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(tabs, BorderLayout.CENTER);

        JButton logout = new JButton("注销");
        logout.addActionListener(e -> cardLayout.show(cardPanel, "login"));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        top.add(new JLabel("身份: " + (permission==2?"管理员":"用户"))); top.add(logout);
        mainPanel.add(top, BorderLayout.NORTH);

        cardPanel.add(mainPanel, "dashboard");
    }

    private JPanel createSearchTab() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        JPanel bar = new JPanel();
        JTextField dep = new JTextField("Paris", 8), arr = new JTextField("London", 8), date = new JTextField("2026.02.01", 8);
        JButton btn = new JButton("搜索");
        bar.add(new JLabel("始发:")); bar.add(dep); bar.add(new JLabel("到达:")); bar.add(arr); bar.add(new JLabel("日期:")); bar.add(date); bar.add(btn);

        ticketModel = new DefaultTableModel(new String[]{"ID", "航班", "起飞", "到达", "经济舱", "商务舱"}, 0);
        JTable table = new JTable(ticketModel);
        p.add(bar, BorderLayout.NORTH); p.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton book = new JButton(" 预 订 ");
        p.add(book, BorderLayout.SOUTH);

        btn.addActionListener(e -> {
            List<Object[]> data = dm.searchTicketsForGUI(dep.getText(), arr.getText(), date.getText(), "none", "none", "none");
            ticketModel.setRowCount(0);
            for (Object[] r : data) ticketModel.addRow(r);
        });

        book.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1 || permission == 0) return;
            int tid = (int) ticketModel.getValueAt(row, 0);
            String[] opts = {"Economy", "Business"};
            int c = JOptionPane.showOptionDialog(this, "选择舱位", "确认", 0, 3, null, opts, opts[0]);
            if (c != -1 && dm.bookTicket(currentUserId, tid, opts[c])) JOptionPane.showMessageDialog(this, "成功");
        });
        return p;
    }

    private JPanel createOrderTab() {
        JPanel p = new JPanel(new BorderLayout());
        orderModel = new DefaultTableModel(new String[]{"ID", "航班", "日期", "舱位"}, 0);
        JTable table = new JTable(orderModel);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        JButton refresh = new JButton("刷新"), del = new JButton("退票");
        JPanel bp = new JPanel(); bp.add(refresh); bp.add(del); p.add(bp, BorderLayout.SOUTH);

        refresh.addActionListener(e -> {
            orderModel.setRowCount(0);
            for (Object[] r : dm.searchOrdersForGUI(currentUserId, permission)) orderModel.addRow(r);
        });
        del.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r != -1 && dm.deleteOrder((int)orderModel.getValueAt(r, 0), permission, currentUserId)) {
                orderModel.removeRow(r);
            }
        });
        return p;
    }

    private JPanel createAdminTab() {
        JPanel p = new JPanel();
        JButton b = new JButton("批量生成未来机票");
        p.add(b);
        b.addActionListener(e -> {
            String r = JOptionPane.showInputDialog("输入范围 (如 2026.06.01 2026.06.05)");
            if (r != null) {
                String[] ds = r.split(" ");
                JOptionPane.showMessageDialog(this, "生成了 " + dm.generateTickets(ds[0], ds[1]) + " 行数据");
            }
        });
        return p;
    }

    public static void main(String[] args) {
        try { FlatLightLaf.setup(); } catch (Exception e) {}
        new MainGUI();
    }
}