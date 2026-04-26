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

        // --- 第一阶段：登录逻辑 ---
        System.out.println("====== 航空订票系统 (CLI 旗舰版) ======");
        while (true) {
            System.out.println("\n请输入手机号 (输入 'visitor' 以游客身份进入):");
            mobile_phone = sc.next();
            if (mobile_phone.equalsIgnoreCase("visitor")) {
                permission = 0;
                System.out.println("已以游客身份进入，部分功能受限。");
                break;
            }

            String password = dm.getPassword(mobile_phone);
            if (password.equals("#")) {
                System.out.println("错误：该手机号未注册！");
                continue;
            }

            System.out.println("请输入密码 (初始用户请直接回车):");
            // 注意：处理空密码可能需要使用 sc.nextLine()，这里简化处理
            String ipt = sc.next();
            if (ipt.equals("none")) ipt = ""; // 假设用户输入 none 代表空

            if (ipt.equals(password)) {
                id = dm.getId(mobile_phone);
                permission = dm.getPermission(mobile_phone);
                System.out.println("登录成功！欢迎回来。");
                break;
            } else {
                System.out.println("密码错误，请重试。");
            }
        }

        // --- 第二阶段：主操作循环 ---
        while (true) {
            System.out.println("\n----------- 功能菜单 -----------");
            System.out.println("1. 航班查询 (搜索 & 订票)");
            System.out.println("2. 订单管理 (查询 & 退票)");
            System.out.println("3. 亲属管理 (添加 & 查看)");
            System.out.println("4. 生成机票 (仅限管理员)");
            System.out.println("5. 修改个人密码");
            System.out.println("6. 退出系统");
            System.out.print(">> 请选择操作: ");
            String choice = sc.next();

            if (choice.equals("1")) {
                // --- 搜索与订票 ---
                System.out.println("输入: 始发地 到达地 日期(YYYY.MM.DD)");
                String dep = sc.next(), arr = sc.next(), date = sc.next();
                List<Object[]> tickets = dm.searchTicketsForGUI(dep, arr, date, "none", "none", "none");

                if (tickets.isEmpty()) {
                    System.out.println("未找到相关航班。");
                } else {
                    System.out.println("ID | 航班号 | 起飞 | 到达 | 经济舱价 | 商务舱价");
                    for (Object[] t : tickets) {
                        System.out.printf("%d | %s | %s | %s | %.2f | %.2f\n", t[0], t[1], t[2], t[3], t[4], t[5]);
                    }

                    if (permission >= 1) {
                        System.out.println("\n是否要订票？输入 TicketID 订票，输入 0 取消:");
                        int tId = sc.nextInt();
                        if (tId != 0) {
                            // --- 代亲属订票逻辑 ---
                            List<Object[]> contacts = dm.getContacts(id);
                            System.out.println("请选择出行人:");
                            System.out.println("0. 本人 (自己)");
                            for (int i = 0; i < contacts.size(); i++) {
                                System.out.println((i + 1) + ". " + contacts.get(i)[1] + " (" + contacts.get(i)[2] + ")");
                            }
                            System.out.print(">> 选择编号: ");
                            int cIndex = sc.nextInt();
                            int targetPassengerId = (cIndex == 0) ? id : (int) contacts.get(cIndex - 1)[0];

                            System.out.println("请选择舱位 (Economy/Business):");
                            String cabin = sc.next();
                            if (dm.bookTicket(targetPassengerId, tId, cabin)) {
                                System.out.println("订票成功！");
                            } else {
                                System.out.println("订票失败，余票不足。");
                            }
                        }
                    }
                }

            } else if (choice.equals("2")) {
                // --- 订单管理 ---
                if (permission < 1) { System.out.println("游客无权管理订单。"); continue; }
                List<Object[]> orders = dm.searchOrdersForGUI(id, permission);
                System.out.println("订单ID | 航班号 | 日期 | 舱位 | 乘机人");
                for (Object[] o : orders) {
                    System.out.printf("%d | %s | %s | %s | %s\n", o[0], o[1], o[2], o[3], o[4]);
                }
                System.out.println("\n输入要退票的订单ID (输入 0 返回):");
                int oId = sc.nextInt();
                if (oId != 0 && dm.deleteOrder(oId, permission, id)) {
                    System.out.println("退票成功。");
                }

            } else if (choice.equals("3")) {
                // --- 亲属管理 ---
                if (permission < 1) { System.out.println("游客无权管理亲属。"); continue; }
                System.out.println("1. 查看亲属列表  2. 添加亲属关系");
                int sub = sc.nextInt();
                if (sub == 1) {
                    List<Object[]> contacts = dm.getContacts(id);
                    System.out.println("姓名 | 关系");
                    for (Object[] c : contacts) System.out.println(c[1] + " | " + c[2]);
                } else if (sub == 2) {
                    System.out.print("请输入亲属的手机号: ");
                    String targetPhone = sc.next();
                    int targetId = dm.getId(targetPhone);
                    if (targetId != -1 && targetId != id) {
                        System.out.print("请输入你们的关系 (如 Friend): ");
                        String type = sc.next();
                        if (dm.addContact(id, targetId, type)) System.out.println("添加成功！");
                    } else {
                        System.out.println("手机号无效或不能添加自己。");
                    }
                }

            } else if (choice.equals("4")) {
                // --- 管理员生成机票 ---
                if (permission < 2) { System.out.println("权限不足！"); continue; }
                System.out.println("输入日期范围 (YYYY.MM.DD YYYY.MM.DD):");
                System.out.println("成功生成 " + dm.generateTickets(sc.next(), sc.next()) + " 行数据。");

            } else if (choice.equals("5")) {
                // --- 修改密码 ---
                if (permission < 1) { System.out.println("游客无权修改密码。"); continue; }
                System.out.println("请输入新密码:");
                dm.updatePassword(id, sc.next());
                System.out.println("密码修改成功！");

            } else if (choice.equals("6")) {
                break;
            }
        }
        dm.closeDatasource();
        System.out.println("感谢使用，再见！");
    }
}