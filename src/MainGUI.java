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

        setTitle("航空订票管理系统");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // UI 微调：圆角和间距
        UIManager.put("Button.arc", 15);
        UIManager.put("Component.arc", 15);
        UIManager.put("TextComponent.arc", 15);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        initLoginPanel();
        add(cardPanel);
        setVisible(true);
    }

    // --- 1. 登录面板 ---
    private void initLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel title = new JLabel("用户登录");
        title.setFont(new Font("微软雅黑", Font.BOLD, 22));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(title, gbc);

        JTextField phoneField = new JTextField("13800138000", 15);
        JPasswordField passField = new JPasswordField(15);
        JButton loginBtn = new JButton(" 登 录 ");
        JButton visitorBtn = new JButton("游客访问");
        JLabel hint = new JLabel("注：初始用户请保持密码为空登录");
        hint.setFont(new Font("微软雅黑", Font.PLAIN, 10));
        hint.setForeground(Color.GRAY);

        gbc.gridwidth = 1; gbc.gridy = 1; panel.add(new JLabel("手机号:"), gbc);
        gbc.gridx = 1; panel.add(phoneField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("密  码:"), gbc);
        gbc.gridx = 1; panel.add(passField, gbc);
        gbc.gridy = 3; panel.add(hint, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; panel.add(loginBtn, gbc);
        gbc.gridy = 5; panel.add(visitorBtn, gbc);

        loginBtn.addActionListener(e -> {
            String phone = phoneField.getText();
            String pwd = new String(passField.getPassword());
            String dbPwd = dm.getPassword(phone);

            if (dbPwd.equals("#")) {
                JOptionPane.showMessageDialog(this, "该手机号未注册");
            } else if (pwd.equals(dbPwd)) {
                currentUserId = dm.getId(phone);
                permission = dm.getPermission(phone);
                initDashboard();
                cardLayout.show(cardPanel, "dashboard");
            } else {
                JOptionPane.showMessageDialog(this, "密码错误");
            }
        });

        visitorBtn.addActionListener(e -> {
            permission = 0; currentUserId = -1; initDashboard(); cardLayout.show(cardPanel, "dashboard");
        });

        cardPanel.add(panel, "login");
    }

    // --- 2. 主面板与选项卡 ---
    private void initDashboard() {
        // 清理旧 Dashboard 面板
        for (Component c : cardPanel.getComponents()) {
            if ("dashboard".equals(c.getName())) cardPanel.remove(c);
        }

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(" 航班查询 ", createSearchTab());
        if (permission >= 1) {
            tabs.addTab(" 我的订单 ", createOrderTab());
            tabs.addTab(" 个人设置 ", createSettingsTab());
        }
        if (permission >= 2) tabs.addTab(" 管理员后台 ", createAdminTab());

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setName("dashboard");
        mainPanel.add(tabs, BorderLayout.CENTER);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        String rName = permission == 2 ? "管理员" : (permission == 1 ? "正式用户" : "游客");
        top.add(new JLabel("身份: " + rName + "  "));
        JButton logout = new JButton("退出登录");
        logout.addActionListener(e -> cardLayout.show(cardPanel, "login"));
        top.add(logout);
        mainPanel.add(top, BorderLayout.NORTH);

        cardPanel.add(mainPanel, "dashboard");
    }

    // --- 搜索标签 ---
    private JPanel createSearchTab() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        JPanel bar = new JPanel();
        JTextField dep = new JTextField("Paris", 8), arr = new JTextField("London", 8), date = new JTextField("2026.02.01", 8);
        JButton btn = new JButton("查询");
        bar.add(new JLabel("出发:")); bar.add(dep);
        bar.add(new JLabel("到达:")); bar.add(arr);
        bar.add(new JLabel("日期:")); bar.add(date);
        bar.add(btn);

        ticketModel = new DefaultTableModel(new String[]{"ID", "航班号", "起飞", "抵达", "经济舱价", "商务舱价"}, 0);
        JTable table = new JTable(ticketModel);
        table.setRowHeight(25);
        p.add(bar, BorderLayout.NORTH);
        p.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton book = new JButton(" 预 订 选 中 航 班 ");
        p.add(book, BorderLayout.SOUTH);

        btn.addActionListener(e -> {
            List<Object[]> data = dm.searchTicketsForGUI(dep.getText(), arr.getText(), date.getText(), "none", "none", "none");
            ticketModel.setRowCount(0);
            for (Object[] r : data) ticketModel.addRow(r);
        });

        book.addActionListener(e -> {
            if (permission == 0) { JOptionPane.showMessageDialog(this, "游客不能订票"); return; }
            int row = table.getSelectedRow();
            if (row == -1) return;
            int tid = (int) ticketModel.getValueAt(row, 0);
            String[] opts = {"Economy", "Business"};
            int c = JOptionPane.showOptionDialog(this, "选择舱位", "订票确认", 0, 3, null, opts, opts[0]);
            if (c != -1 && dm.bookTicket(currentUserId, tid, opts[c])) JOptionPane.showMessageDialog(this, "订票成功");
        });
        return p;
    }

    // --- 订单标签 ---
    private JPanel createOrderTab() {
        JPanel p = new JPanel(new BorderLayout());
        orderModel = new DefaultTableModel(new String[]{"订单ID", "航班号", "日期", "舱位"}, 0);
        JTable table = new JTable(orderModel);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        JButton refresh = new JButton("刷新列表"), del = new JButton("申请退票");
        JPanel bp = new JPanel(); bp.add(refresh); bp.add(del);
        p.add(bp, BorderLayout.SOUTH);

        refresh.addActionListener(e -> {
            orderModel.setRowCount(0);
            for (Object[] r : dm.searchOrdersForGUI(currentUserId, permission)) orderModel.addRow(r);
        });
        del.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1 && dm.deleteOrder((int)orderModel.getValueAt(row, 0), permission, currentUserId)) {
                orderModel.removeRow(row);
            }
        });
        return p;
    }

    // --- 个人设置（修改密码）标签 ---
    private JPanel createSettingsTab() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 10, 10, 10);

        JPasswordField p1 = new JPasswordField(15), p2 = new JPasswordField(15);
        JButton b = new JButton("修改并保存新密码");

        g.gridx = 0; g.gridy = 0; p.add(new JLabel("新 密 码:"), g);
        g.gridx = 1; p.add(p1, g);
        g.gridx = 0; g.gridy = 1; p.add(new JLabel("确认密码:"), g);
        g.gridx = 1; p.add(p2, g);
        g.gridx = 0; g.gridy = 2; g.gridwidth = 2; p.add(b, g);

        b.addActionListener(e -> {
            String pass1 = new String(p1.getPassword()), pass2 = new String(p2.getPassword());
            if (pass1.isEmpty()) return;
            if (pass1.equals(pass2)) {
                dm.updatePassword(currentUserId, pass1);
                JOptionPane.showMessageDialog(this, "修改成功，请牢记新密码");
                p1.setText(""); p2.setText("");
            } else JOptionPane.showMessageDialog(this, "两次输入不一致");
        });
        return p;
    }

    // --- 管理员标签 ---
    private JPanel createAdminTab() {
        JPanel p = new JPanel();
        JButton b = new JButton("批量生成未来机票 (Generate Tickets)");
        p.add(b);
        b.addActionListener(e -> {
            String r = JOptionPane.showInputDialog("输入范围 (如 2026.06.01 2026.06.05)");
            if (r != null && r.contains(" ")) {
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