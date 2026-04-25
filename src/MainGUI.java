import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

public class MainGUI extends JFrame {
    private DatabaseManipulation dm;
    private int currentUserId = -1;
    private int permission = 0; // 0: Visitor, 1: User, 2: Admin

    private JPanel cardPanel;
    private CardLayout cardLayout;
    private DefaultTableModel ticketTableModel;
    private DefaultTableModel orderTableModel;

    public MainGUI() {
        dm = new DatabaseManipulation();
        dm.openDatasource();

        // 基础窗口设置
        setTitle("航空订票系统 - Proj 1");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // UI 美化细节
        UIManager.put("Button.arc", 15);
        UIManager.put("Component.arc", 15);
        UIManager.put("TextComponent.arc", 15);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        initLoginPanel();
        // 注意：dashboard 会在登录成功后重新初始化以加载权限
        add(cardPanel);
        setVisible(true);
    }

    // --- 1. 登录面板 ---
    private void initLoginPanel() {
        JPanel loginPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel titleLabel = new JLabel("欢迎使用航空订票系统");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 24));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        loginPanel.add(titleLabel, gbc);

        JTextField phoneField = new JTextField(18);
        JPasswordField passField = new JPasswordField(18);
        JButton loginBtn = new JButton("立即登录");
        JButton visitorBtn = new JButton("以游客身份进入");

        gbc.gridwidth = 1; gbc.gridy = 1; loginPanel.add(new JLabel("手机号:"), gbc);
        gbc.gridx = 1; loginPanel.add(phoneField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; loginPanel.add(new JLabel("密  码:"), gbc);
        gbc.gridx = 1; loginPanel.add(passField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        loginPanel.add(loginBtn, gbc);
        gbc.gridy = 4;
        loginPanel.add(visitorBtn, gbc);

        // 登录逻辑
        loginBtn.addActionListener(e -> {
            String phone = phoneField.getText();
            String pwd = new String(passField.getPassword());
            String dbPwd = dm.getPassword(phone);

            if (dbPwd.equals(pwd)) {
                currentUserId = dm.getId(phone);
                permission = dm.getPermission(phone);
                initMainDashboard();
                cardLayout.show(cardPanel, "dashboard");
            } else {
                JOptionPane.showMessageDialog(this, "手机号或密码错误！", "登录失败", JOptionPane.ERROR_MESSAGE);
            }
        });

        visitorBtn.addActionListener(e -> {
            permission = 0;
            initMainDashboard();
            cardLayout.show(cardPanel, "dashboard");
        });

        cardPanel.add(loginPanel, "login");
    }

    // --- 2. 主仪表盘（选项卡结构） ---
    private void initMainDashboard() {
        JTabbedPane tabs = new JTabbedPane();

        // 标签页 1: 搜索与订票
        tabs.addTab(" 航班查询 & 订票 ", createSearchTab());

        // 标签页 2: 订单管理 (游客不可见内容)
        if (permission >= 1) {
            tabs.addTab(" 我的订单 ", createOrderTab());
        }

        // 标签页 3: 管理员工具
        if (permission >= 2) {
            tabs.addTab(" 管理员后台 ", createAdminTab());
        }

        JPanel dashboard = new JPanel(new BorderLayout());
        dashboard.add(tabs, BorderLayout.CENTER);

        // 顶部状态条
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        statusPanel.add(new JLabel("当前身份: " + (permission==0?"游客":permission==2?"管理员":"正式用户")));
        JButton logoutBtn = new JButton("退出登录");
        logoutBtn.addActionListener(e -> cardLayout.show(cardPanel, "login"));
        statusPanel.add(logoutBtn);
        dashboard.add(statusPanel, BorderLayout.NORTH);

        cardPanel.add(dashboard, "dashboard");
    }

    // --- 搜索标签页内容 ---
    private JPanel createSearchTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // 搜索栏
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField depField = new JTextField("Paris", 8);
        JTextField arrField = new JTextField("London", 8);
        JTextField dateField = new JTextField("2026.02.01", 10);
        JButton searchBtn = new JButton("搜索航班");
        searchBar.add(new JLabel("出发:")); searchBar.add(depField);
        searchBar.add(new JLabel("到达:")); searchBar.add(arrField);
        searchBar.add(new JLabel("日期:")); searchBar.add(dateField);
        searchBar.add(searchBtn);

        // 表格
        String[] headers = {"ID", "航班号", "起飞时间", "抵达时间", "经济舱价", "商务舱价"};
        ticketTableModel = new DefaultTableModel(headers, 0);
        JTable table = new JTable(ticketTableModel);
        table.setRowHeight(30);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        // 订票按钮
        JButton bookBtn = new JButton("预订选中的航班");
        panel.add(bookBtn, BorderLayout.SOUTH);

        searchBtn.addActionListener(e -> {
            // 这里建议修改你的 dm.searchTickets 让它直接更新 tableModel，或者手动模拟
            dm.searchTickets(depField.getText(), arrField.getText(), dateField.getText(), "none", "none", "none");
            JOptionPane.showMessageDialog(this, "已在控制台输出搜索结果，请查看表格同步逻辑");
            // 演示：你可以通过改写 dm 方法让数据直接进 GUI
        });

        bookBtn.addActionListener(e -> {
            if (permission == 0) {
                JOptionPane.showMessageDialog(this, "请先登录后再订票！");
                return;
            }
            int row = table.getSelectedRow();
            if (row != -1) {
                int ticketId = (int) table.getValueAt(row, 0);
                String[] options = {"Economy", "Business"};
                int choice = JOptionPane.showOptionDialog(this, "请选择舱位", "订票确认",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                if (choice != -1) {
                    dm.bookTicket(currentUserId, ticketId, options[choice]);
                }
            }
        });

        return panel;
    }

    // --- 订单标签页内容 ---
    private JPanel createOrderTab() {
        JPanel panel = new JPanel(new BorderLayout());
        orderTableModel = new DefaultTableModel(new String[]{"订单ID", "航班号", "日期", "舱位"}, 0);
        JTable table = new JTable(orderTableModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton refreshBtn = new JButton("刷新订单");
        JButton deleteBtn = new JButton("退票");
        btnPanel.add(refreshBtn); btnPanel.add(deleteBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        refreshBtn.addActionListener(e -> {
            orderTableModel.setRowCount(0);
            dm.searchOrders(currentUserId, permission);
            // 注意：需要修改 dm.searchOrders 让它返回列表或在此更新 UI
        });

        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                int orderId = (int) table.getValueAt(row, 0);
                if (JOptionPane.showConfirmDialog(this, "确定要退票吗？") == JOptionPane.YES_OPTION) {
                    dm.deleteOrder(orderId, permission, currentUserId);
                }
            }
        });

        return panel;
    }

    // --- 管理员标签页内容 ---
    private JPanel createAdminTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        JButton genBtn = new JButton("批量生成未来机票 (Generate Tickets)");
        genBtn.setPreferredSize(new Dimension(300, 50));
        panel.add(genBtn);

        genBtn.addActionListener(e -> {
            String range = JOptionPane.showInputDialog(this, "请输入日期范围 (例如: 2026.06.01 2026.06.05)");
            if (range != null && range.contains(" ")) {
                String[] dates = range.split(" ");
                int rows = dm.generateTickets(dates[0], dates[1]);
                JOptionPane.showMessageDialog(this, "成功生成 " + rows + " 条机票记录！");
            }
        });

        return panel;
    }

    public static void main(String[] args) {
        try {
            // 使用 FlatLaf 现代皮肤
            com.formdev.flatlaf.FlatLightLaf.setup();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new MainGUI();
        });
    }
}