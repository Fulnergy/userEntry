import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DatabaseManipulation implements DataManipulation {
    private Connection con = null;
    private String host = "localhost";
    private String dbname = "proj1";
    private String user = "postgres";
    private String pwd = "258066"; // <--- 改成你设置的那个密码
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
    public void searchOrders(int passengerId) {
        String sql = "SELECT o.order_id, f.flight_number, t.flight_date, o.cabin_class FROM ticket_order o " +
                "JOIN ticket t ON o.ticket_id = t.ticket_id JOIN flight f ON t.flight_id = f.id WHERE o.passenger_id = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, passengerId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                System.out.printf("Order ID: %d | Flight: %s | Date: %s | Class: %s\n",
                        rs.getInt(1), rs.getString(2), rs.getDate(3), rs.getString(4));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public boolean deleteOrder(int orderId) {
        String sql = "DELETE FROM ticket_order WHERE order_id = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // 电影相关方法实现（留空以兼容之前的接口）
    public int addOneMovie(String s) {return 0;}
    public String allContinentNames() {return null;}
    public String continentsWithCountryCount() {return null;}
    public String FullInformationOfMoviesRuntime(int i1, int i2) {return null;}
    public String findMovieById(int i) {return null;}
}