import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManipulation implements DataManipulation {
    private Connection con = null;

    private int developer = 0;
    private String dbname = developer==1 ? "project" : "proj1";
    private String user = developer==1 ? "checker" : "postgres";
    private String pwd = developer==1 ? "123455" : "258066";
    @Override
    public void openDatasource() {
        try {
            Class.forName("org.postgresql.Driver");
            String url = "jdbc:postgresql://localhost:5432/" + dbname;
            con = DriverManager.getConnection(url, user, pwd);
            System.out.println("数据库连接成功");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void closeDatasource() {
        try { if (con != null) con.close(); } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public String getPassword(String mobile_phone){
        String sql = "SELECT password FROM passenger WHERE mobile_phone = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, mobile_phone);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString(1) : "#";
        } catch (SQLException e) { return "$"; }
    }

    @Override
    public int getPermission(String mobile_phone){
        String sql = "SELECT permission FROM passenger WHERE mobile_phone = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, mobile_phone);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) { return 0; }
    }

    @Override
    public int getId(String mobile_phone){
        String sql = "SELECT id FROM passenger WHERE mobile_phone = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, mobile_phone);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) { return 0; }
    }

    @Override
    public int generateTickets(String startDate, String endDate) {
        int totalRows = 0;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        LocalDate start = LocalDate.parse(startDate, formatter);
        LocalDate end = LocalDate.parse(endDate, formatter);
        String sql = "INSERT INTO ticket (flight_id, flight_date, business_price, business_remain, economy_price, economy_remain) " +
                "SELECT id, ?, 2000.0, 20, 1000.0, 100 FROM flight ON CONFLICT DO NOTHING";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                pstmt.setDate(1, Date.valueOf(date));
                totalRows += pstmt.executeUpdate();
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return totalRows;
    }

    @Override
    public List<Object[]> searchTicketsForGUI(String depCity, String arrCity, String date, String airline, String depTime, String arrTime) {
        List<Object[]> results = new ArrayList<>();
        StringBuilder sb = new StringBuilder(
                "SELECT t.ticket_id, f.flight_number, f.scheduled_departure_time, f.scheduled_arrival_time, " +
                        "t.economy_price, t.business_price FROM ticket t " +
                        "JOIN flight f ON t.flight_id = f.id " +
                        "JOIN airport a_src ON f.source_iata_code = a_src.iata_code " +
                        "JOIN city c_src ON a_src.city_id = c_src.id " +
                        "JOIN airport a_dest ON f.dest_iata_code = a_dest.iata_code " +
                        "JOIN city c_dest ON a_dest.city_id = c_dest.id " +
                        "JOIN airline al ON f.airline_id = al.id " +
                        "WHERE c_src.city_name = ? AND c_dest.city_name = ? AND t.flight_date = ?"
        );
        if (airline != null && !airline.equalsIgnoreCase("none")) sb.append(" AND al.airline_name = '").append(airline).append("'");

        try (PreparedStatement pstmt = con.prepareStatement(sb.toString())) {
            pstmt.setString(1, depCity); pstmt.setString(2, arrCity);
            pstmt.setDate(3, Date.valueOf(date.replace('.', '-')));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                results.add(new Object[]{ rs.getInt(1), rs.getString(2), rs.getTime(3), rs.getTime(4), rs.getDouble(5), rs.getDouble(6) });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return results;
    }

    @Override
    public boolean bookTicket(int passengerId, int ticketId, String cabinClass) {
        try {
            con.setAutoCommit(false);
            String col = cabinClass.equalsIgnoreCase("Economy") ? "economy_remain" : "business_remain";
            String updateSql = "UPDATE ticket SET " + col + " = " + col + " - 1 WHERE ticket_id = ? AND " + col + " > 0";
            try (PreparedStatement ps1 = con.prepareStatement(updateSql)) {
                ps1.setInt(1, ticketId);
                if (ps1.executeUpdate() == 0) { con.rollback(); return false; }
            }
            String insSql = "INSERT INTO ticket_order (passenger_id, ticket_id, cabin_class) VALUES (?, ?, ?)";
            try (PreparedStatement ps2 = con.prepareStatement(insSql)) {
                ps2.setInt(1, passengerId); ps2.setInt(2, ticketId); ps2.setString(3, cabinClass);
                ps2.executeUpdate();
            }
            con.commit();
            return true;
        } catch (Exception e) {
            try { con.rollback(); } catch (SQLException ex) {}
            return false;
        } finally { try { con.setAutoCommit(true); } catch (SQLException e) {} }
    }

    @Override
    public List<Object[]> searchOrdersForGUI(int passengerId, int permission) {
        List<Object[]> results = new ArrayList<>();
        String sql = "SELECT o.order_id, f.flight_number, t.flight_date, o.cabin_class FROM ticket_order o " +
                "JOIN ticket t ON o.ticket_id = t.ticket_id JOIN flight f ON t.flight_id = f.id";
        if(permission <= 1) sql += " WHERE o.passenger_id = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            if(permission <= 1) pstmt.setInt(1, passengerId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                results.add(new Object[]{ rs.getInt(1), rs.getString(2), rs.getDate(3), rs.getString(4) });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return results;
    }

    @Override
    public boolean deleteOrder(int orderId, int permission, int passengerId) {
        String sql = "DELETE FROM ticket_order WHERE order_id = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    @Override
    public void updatePassword(int id, String password) {
        String sql = "UPDATE passenger SET password = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, password); ps.setInt(2, id); ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // 兼容方法留空...
    public int addOneMovie(String s) {return 0;}
    public String allContinentNames() {return null;}
    public String continentsWithCountryCount() {return null;}
    public String FullInformationOfMoviesRuntime(int i, int j) {return null;}
    public String findMovieById(int i) {return null;}
}