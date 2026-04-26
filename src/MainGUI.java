import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Vector;

public class MainGUI extends JFrame {
    private DatabaseManipulation dm;
    private int currentUserId = -1;
    private int permission = 0;
    private JPanel cardPanel;
    private CardLayout cardLayout;

    public MainGUI() {
        dm = new DatabaseManipulation();
        dm.openDatasource();
        setTitle("航空系统 Proj 1 - 旗舰版");
        setSize(1100, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        UIManager.put("Button.arc", 15);
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        initLoginPanel();
        add(cardPanel);
        setVisible(true);
    }

    private void initLoginPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints(); g.insets = new Insets(10,10,10,10);
        JTextField pf = new JTextField("", 15); JPasswordField psf = new JPasswordField(15);
        JButton btn = new JButton(" 登 录 "); JButton vBtn = new JButton("游客模式");
        g.gridx=0; g.gridy=0; p.add(new JLabel("手机号:"), g); g.gridx=1; p.add(pf, g);
        g.gridx=0; g.gridy=1; p.add(new JLabel("密  码:"), g); g.gridx=1; p.add(psf, g);
        g.gridx=0; g.gridy=2; g.gridwidth=2; p.add(btn, g); g.gridy=3; p.add(vBtn, g);

        btn.addActionListener(e -> {
            String dbP = dm.getPassword(pf.getText());
            if (new String(psf.getPassword()).equals(dbP)) {
                currentUserId = dm.getId(pf.getText()); permission = dm.getPermission(pf.getText());
                initDashboard(); cardLayout.show(cardPanel, "dashboard");
            } else JOptionPane.showMessageDialog(this, "登录失败");
        });
        vBtn.addActionListener(e -> { permission=0; initDashboard(); cardLayout.show(cardPanel, "dashboard"); });
        cardPanel.add(p, "login");
    }

    private void initDashboard() {
        for (Component c : cardPanel.getComponents()) if ("dashboard".equals(c.getName())) cardPanel.remove(c);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(" 航班查询 ", createSearchTab());
        if (permission >= 1) {
            tabs.addTab(" 我的订单 ", createOrderTab());
            tabs.addTab(" 亲属管理 ", createContactTab());
            tabs.addTab(" 个人设置 ", createSettingsTab());
        }
        if (permission >= 2) tabs.addTab(" 管理员后台 ", createAdminTab());

        JPanel main = new JPanel(new BorderLayout());
        main.setName("dashboard");
        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        top.add(new JLabel("权限: " + (permission==2?"管理员":"用户")));
        JButton logout = new JButton("注销"); logout.addActionListener(e -> cardLayout.show(cardPanel, "login"));
        top.add(logout); main.add(top, BorderLayout.NORTH); main.add(tabs, BorderLayout.CENTER);
        cardPanel.add(main, "dashboard");
    }

    private JPanel createSearchTab() {
        JPanel p = new JPanel(new BorderLayout(10,10));
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "航班", "起飞", "到达", "经济", "商务"}, 0);
        JTable table = new JTable(model);
        JTextField dep = new JTextField("Paris", 8), arr = new JTextField("London", 8), d = new JTextField("2026.02.01", 8);
        JButton sBtn = new JButton("查询"), bBtn = new JButton(" 订 票 ");
        JPanel bar = new JPanel(); bar.add(new JLabel("始发:")); bar.add(dep); bar.add(new JLabel("到达:")); bar.add(arr); bar.add(new JLabel("日期:")); bar.add(d); bar.add(sBtn);
        p.add(bar, BorderLayout.NORTH); p.add(new JScrollPane(table), BorderLayout.CENTER); p.add(bBtn, BorderLayout.SOUTH);

        sBtn.addActionListener(e -> {
            model.setRowCount(0);
            for (Object[] r : dm.searchTicketsForGUI(dep.getText(), arr.getText(), d.getText(), "none", "none", "none")) model.addRow(r);
        });

        bBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1 || permission == 0) { JOptionPane.showMessageDialog(this, "无效选择或权限"); return; }

            // 亲属订票逻辑
            List<Object[]> contacts = dm.getContacts(currentUserId);
            Vector<String> names = new Vector<>(); names.add("本人 (自己)");
            for (Object[] c : contacts) names.add(c[1] + " (" + c[2] + ")");

            JComboBox<String> nameBox = new JComboBox<>(names);
            JComboBox<String> cabinBox = new JComboBox<>(new String[]{"Economy", "Business"});
            Object[] msg = {"选择出行人:", nameBox, "选择舱位:", cabinBox};

            if (JOptionPane.showConfirmDialog(this, msg, "订票确认", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                int targetId = (nameBox.getSelectedIndex() == 0) ? currentUserId : (int) contacts.get(nameBox.getSelectedIndex()-1)[0];
                if (dm.bookTicket(targetId, (int)model.getValueAt(row, 0), (String)cabinBox.getSelectedItem()))
                    JOptionPane.showMessageDialog(this, "订票成功");
            }
        });
        return p;
    }

    private JPanel createContactTab() {
        JPanel p = new JPanel(new BorderLayout());
        DefaultTableModel m = new DefaultTableModel(new String[]{"ID", "姓名", "关系"}, 0);
        JTable t = new JTable(m); p.add(new JScrollPane(t), BorderLayout.CENTER);
        JButton add = new JButton("添加亲属关系"); p.add(add, BorderLayout.SOUTH);

        add.addActionListener(e -> {
            String phone = JOptionPane.showInputDialog("请输入对方手机号:");
            if (phone != null) {
                int tid = dm.getId(phone);
                if (tid != -1 && tid != currentUserId) {
                    String rel = JOptionPane.showInputDialog("你们的关系 (如 Parent):");
                    if (dm.addContact(currentUserId, tid, rel)) {
                        JOptionPane.showMessageDialog(this, "添加成功");
                        m.setRowCount(0); for (Object[] r : dm.getContacts(currentUserId)) m.addRow(r);
                    }
                }
            }
        });
        return p;
    }

    private JPanel createOrderTab() {
        JPanel p = new JPanel(new BorderLayout());
        DefaultTableModel m = new DefaultTableModel(new String[]{"ID", "航班", "日期", "舱位", "乘客名"}, 0);
        JTable t = new JTable(m); p.add(new JScrollPane(t), BorderLayout.CENTER);
        JButton ref = new JButton("刷新"), del = new JButton("退票");
        JPanel bp = new JPanel(); bp.add(ref); bp.add(del); p.add(bp, BorderLayout.SOUTH);
        ref.addActionListener(e -> {
            m.setRowCount(0); for (Object[] r : dm.searchOrdersForGUI(currentUserId, permission)) m.addRow(r);
        });
        del.addActionListener(e -> {
            int r = t.getSelectedRow();
            if (r != -1 && dm.deleteOrder((int)m.getValueAt(r,0), permission, currentUserId)) m.removeRow(r);
        });
        return p;
    }

    private JPanel createSettingsTab() {
        JPanel p = new JPanel(new GridBagLayout()); GridBagConstraints g = new GridBagConstraints(); g.insets = new Insets(10,10,10,10);
        JPasswordField p1 = new JPasswordField(15), p2 = new JPasswordField(15); JButton b = new JButton("确认修改");
        g.gridx=0; g.gridy=0; p.add(new JLabel("新密码:"), g); g.gridx=1; p.add(p1, g);
        g.gridx=0; g.gridy=1; p.add(new JLabel("重复密码:"), g); g.gridx=1; p.add(p2, g);
        g.gridx=0; g.gridy=2; g.gridwidth=2; p.add(b, g);
        b.addActionListener(e -> {
            if (new String(p1.getPassword()).equals(new String(p2.getPassword()))) {
                dm.updatePassword(currentUserId, new String(p1.getPassword())); JOptionPane.showMessageDialog(this, "修改成功");
            }
        });
        return p;
    }

    private JPanel createAdminTab() {
        JPanel p = new JPanel(); JButton b = new JButton("生成未来机票"); p.add(b);
        b.addActionListener(e -> {
            String r = JOptionPane.showInputDialog("范围 (如 2026.06.01 2026.06.05)");
            if (r != null) JOptionPane.showMessageDialog(this, "生成了 " + dm.generateTickets(r.split(" ")[0], r.split(" ")[1]) + " 行");
        });
        return p;
    }

    public static void main(String[] args) {
        try { FlatLightLaf.setup(); } catch (Exception e) {}
        new MainGUI();
    }
}