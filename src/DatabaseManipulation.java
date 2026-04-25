import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DatabaseManipulation implements DataManipulation {
    private int developer = 0;
    private Connection con = null;
    private String host = "localhost";
    private String dbname = developer==1 ? "project" : "proj1";
    private String user = developer==1 ? "checker" : "postgres";
    private String pwd = developer==1 ? "123455" : "258066";
    private String port = "5432";

    @Override
    public void openDatasource() {
        try {
            Class.forName("org.postgresql.Driver");
            String url = "jdbc:postgresql://" + host + ":" + port + "/" + dbname;
            con = DriverManager.getConnection(url, user, pwd);
            System.out.println("Database connected successfully.");
        } catch (Exception e) {
            System.err.println("Database connection failed: " + e.getMessage());
            System.exit(1);
        }
    }

    @Override
    public void closeDatasource() {
        try { if (con != null) con.close(); } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public String getPassword(String mobile_phone){
        String sql = "SELECT password FROM passenger where mobile_phone = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1,mobile_phone);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                return rs.getString(1);
            }
            else return "#";
        } catch (SQLException e) {
            return "$";
        }
    }

    @Override
    public int getPermission(String mobile_phone){
        String sql = "SELECT permission FROM passenger where mobile_phone = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1,mobile_phone);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                return rs.getInt(1);
            }
            else return 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    @Override
    public int getId(String mobile_phone){
        String sql = "SELECT id FROM passenger where mobile_phone = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1,mobile_phone);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                return rs.getInt(1);
            }
            else return 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    @Override
    public int generateTickets(String startDate, String endDate) {
        int totalRows = 0;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        LocalDate start = LocalDate.parse(startDate, formatter);
        LocalDate end = LocalDate.parse(endDate, formatter);

        // 注意：这里使用的是你的建表语句中的 ticket 字段名
        String sql = "INSERT INTO ticket (flight_id, flight_date, business_price, business_remain, economy_price, economy_remain) " +
                "SELECT id, ?, 2000.0, 20, 1000.0, 100 FROM flight " +
                "ON CONFLICT DO NOTHING";

        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                pstmt.setDate(1, Date.valueOf(date));
                totalRows += pstmt.executeUpdate();
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return totalRows;
    }

    @Override
    public void searchTickets(String depCity, String arrCity, String date, String airline, String depTime, String arrTime) {
        // 修正后的 SQL：通过 airport 表中转连接 city 表
        StringBuilder sb = new StringBuilder(
                "SELECT t.ticket_id, f.flight_number, f.scheduled_departure_time, f.scheduled_arrival_time, " +
                        "t.economy_price, t.business_price " +
                        "FROM ticket t " +
                        "JOIN flight f ON t.flight_id = f.id " +
                        "JOIN airport a_src ON f.source_iata_code = a_src.iata_code " +
                        "JOIN city c_src ON a_src.city_id = c_src.id " +
                        "JOIN airport a_dest ON f.dest_iata_code = a_dest.iata_code " +
                        "JOIN city c_dest ON a_dest.city_id = c_dest.id " +
                        "JOIN airline al ON f.airline_id = al.id " +
                        "WHERE c_src.city_name = ? AND c_dest.city_name = ? AND t.flight_date = ?"
        );

        if (airline != null && !airline.equalsIgnoreCase("none")) sb.append(" AND al.airline_name = '").append(airline).append("'");
        if (depTime != null && !depTime.equalsIgnoreCase("none")) sb.append(" AND f.scheduled_departure_time >= '").append(depTime).append("'");
        if (arrTime != null && !arrTime.equalsIgnoreCase("none")) sb.append(" AND f.scheduled_arrival_time <= '").append(arrTime).append("'");

        try (PreparedStatement pstmt = con.prepareStatement(sb.toString())) {
            pstmt.setString(1, depCity);
            pstmt.setString(2, arrCity);
            pstmt.setDate(3, Date.valueOf(date.replace('.', '-')));
            ResultSet rs = pstmt.executeQuery();
            System.out.println("\nID | Flight | DepTime | ArrTime | Economy | Business");
            System.out.println("-----------------------------------------------------");
            while (rs.next()) {
                System.out.printf("%d | %s | %s | %s | %.2f | %.2f\n",
                        rs.getInt(1), rs.getString(2), rs.getTime(3), rs.getTime(4), rs.getDouble(5), rs.getDouble(6));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public boolean bookTicket(int passengerId, int ticketId, String cabinClass) {
        try {
            con.setAutoCommit(false); // 事务开始
            String col = cabinClass.equalsIgnoreCase("Economy") ? "economy_remain" : "business_remain";
            String updateSql = "UPDATE ticket SET " + col + " = " + col + " - 1 WHERE ticket_id = ? AND " + col + " > 0";

            try (PreparedStatement ps1 = con.prepareStatement(updateSql)) {
                ps1.setInt(1, ticketId);
                if (ps1.executeUpdate() == 0) {
                    con.rollback();
                    System.out.println("No seats left!");
                    return false;
                }
            }
            String insSql = "INSERT INTO ticket_order (passenger_id, ticket_id, cabin_class) VALUES (?, ?, ?)";
            try (PreparedStatement ps2 = con.prepareStatement(insSql)) {
                ps2.setInt(1, passengerId);
                ps2.setInt(2, ticketId);
                ps2.setString(3, cabinClass);
                ps2.executeUpdate();
            }
            con.commit();
            System.out.println("Booking successful!");
            return true;
        } catch (Exception e) {
            try { con.rollback(); } catch (SQLException ex) {}
            e.printStackTrace();
            return false;
        } finally {
            try { con.setAutoCommit(true); } catch (SQLException e) {}
        }
    }

    @Override
    public void searchOrders(int passengerId, int permission) {
        String sql = "SELECT o.order_id, f.flight_number, t.flight_date, o.cabin_class FROM ticket_order o " +
                "JOIN ticket t ON o.ticket_id = t.ticket_id JOIN flight f ON t.flight_id = f.id";
        if(permission<=1){
            sql += " WHERE o.passenger_id = ?";
        }
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            if(permission<=1)pstmt.setInt(1, passengerId);
            ResultSet rs = pstmt.executeQuery();
            int cnt = 0;
            while (rs.next()) {
                System.out.printf("Order ID: %d | Flight: %s | Date: %s | Class: %s\n",
                        rs.getInt(1), rs.getString(2), rs.getDate(3), rs.getString(4));
                cnt++;
            }
            if(cnt==0) System.out.println("You have no ticket order");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public boolean deleteOrder(int orderId, int permission, int passengerId) {
        String s = "SELECT passenger_id FROM ticket_order WHERE order_id = ?";
        try (PreparedStatement ps = con.prepareStatement(s)) {
            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();
            if(!rs.next()){
                System.out.println("No such order exist");
                return false;
            }
            if(passengerId!=rs.getInt(1)&&permission<=1){
                System.out.println("\nYou have no permission to delete order not belongs to you");
                return false;
            }
        } catch (SQLException e) { e.printStackTrace(); return false; }
        String sql = "DELETE FROM ticket_order WHERE order_id = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            int ret = pstmt.executeUpdate();
            if(ret>0) System.out.println("Delete succeed!");
            return ret > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public void updatePassword(int id, String password) {
        String sql = "UPDATE passenger SET password = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, password);
            ps.setInt(2, id);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                System.out.println("未找到 id 为 " + id + " 的乘客");
            }
        } catch (SQLException e) {
            System.out.println("更新密码失败");
        }
    }

    // 电影相关方法实现（留空以兼容之前的接口）
    public int addOneMovie(String s) {return 0;}
    public String allContinentNames() {return null;}
    public String continentsWithCountryCount() {return null;}
    public String FullInformationOfMoviesRuntime(int i1, int i2) {return null;}
    public String findMovieById(int i) {return null;}
}