import java.util.List;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        DatabaseManipulation dm = new DatabaseManipulation();
        dm.openDatasource();
        Scanner sc = new Scanner(System.in);

        int id = -1;
        int permission = 0;
        String mobile_phone = "";

        // --- 登录部分 ---
        System.out.println("=== 欢迎使用航空系统 (命令行版) ===");
        while (true) {
            System.out.println("请输入手机号 (输入 visitor 以游客进入):");
            mobile_phone = sc.next();
            if (mobile_phone.equals("visitor")) break;

            String password = dm.getPassword(mobile_phone);
            if (password.equals("#")) {
                System.out.println("用户不存在！");
                continue;
            }
            System.out.println("请输入密码:");
            String ipt = sc.next();
            if (ipt.equals(password)) {
                id = dm.getId(mobile_phone);
                permission = dm.getPermission(mobile_phone);
                System.out.println("登录成功！");
                break;
            } else {
                System.out.println("密码错误！");
            }
        }

        // --- 主操作循环 ---
        while (true) {
            System.out.println("\n--- 功能菜单 ---");
            System.out.println("1. 生成机票 (Admin)");
            System.out.println("2. 搜索航班");
            System.out.println("3. 订单管理");
            System.out.println("4. 退出");
            System.out.print(">> 输入选项: ");
            String input = sc.next();

            if (input.equals("1")) {
                if (permission < 2) { System.out.println("权限不足！"); continue; }
                System.out.println("输入日期范围 (YYYY.MM.DD YYYY.MM.DD):");
                System.out.println("成功生成 " + dm.generateTickets(sc.next(), sc.next()) + " 行数据。");

            } else if (input.equals("2")) {
                System.out.println("输入: 始发地 到达地 日期(YYYY.MM.DD)");
                List<Object[]> results = dm.searchTicketsForGUI(sc.next(), sc.next(), sc.next(), "none", "none", "none");
                System.out.println("ID | 航班 | 起飞 | 到达 | 价格");
                for (Object[] r : results) {
                    System.out.printf("%d | %s | %s | %s | %.2f\n", r[0], r[1], r[2], r[3], r[4]);
                }

            } else if (input.equals("3")) {
                if (permission < 1) { System.out.println("游客无法管理订单！"); continue; }
                System.out.println("1. 查看订单  2. 订票  3. 退票");
                int sub = sc.nextInt();
                if (sub == 1) {
                    List<Object[]> orders = dm.searchOrdersForGUI(id, permission);
                    for (Object[] o : orders) System.out.printf("订单ID: %d | 航班: %s | 日期: %s | 舱位: %s\n", o[0], o[1], o[2], o[3]);
                } else if (sub == 2) {
                    System.out.println("输入 TicketID 和 舱位(Economy/Business):");
                    dm.bookTicket(id, sc.nextInt(), sc.next());
                } else if (sub == 3) {
                    System.out.println("输入要删除的 OrderID:");
                    dm.deleteOrder(sc.nextInt(), permission, id);
                }
            } else if (input.equals("4")) break;
        }
        dm.closeDatasource();
    }
}