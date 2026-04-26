
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
                con = DriverManager.getConnection("jdbc:postgresql://localhost:5432/" + dbname, user, pwd);
            } catch (Exception e) { e.printStackTrace(); System.exit(1); }
        }

        @Override
        public void closeDatasource() {
            try { if (con != null) con.close(); } catch (SQLException e) { e.printStackTrace(); }
        }

        @Override
        public String getPassword(String phone) {
            String sql = "SELECT password FROM passenger WHERE mobile_phone = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, phone);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) { String p = rs.getString(1); return (p == null) ? "" : p; }
                return "#";
            } catch (SQLException e) { return "$"; }
        }

        @Override
        public int getId(String phone) {
            String sql = "SELECT id FROM passenger WHERE mobile_phone = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, phone);
                ResultSet rs = ps.executeQuery();
                return rs.next() ? rs.getInt(1) : -1;
            } catch (SQLException e) { return -1; }
        }

        @Override
        public int getPermission(String phone) {
            String sql = "SELECT permission FROM passenger WHERE mobile_phone = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, phone);
                ResultSet rs = ps.executeQuery();
                return rs.next() ? rs.getInt(1) : 0;
            } catch (SQLException e) { return 0; }
        }

        @Override
        public void updatePassword(int id, String pwd) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE passenger SET password = ? WHERE id = ?")) {
                ps.setString(1, pwd); ps.setInt(2, id); ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        }

        @Override
        public List<Object[]> getContacts(int passengerId) {
            List<Object[]> res = new ArrayList<>();
            String sql = "SELECT p.id, p.name, c.relationship_type FROM contacts c " +
                    "JOIN passenger p ON c.passenger_id2 = p.id WHERE c.passenger_id1 = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, passengerId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) res.add(new Object[]{ rs.getInt(1), rs.getString(2), rs.getString(3) });
            } catch (SQLException e) { e.printStackTrace(); }
            return res;
        }

        @Override
        public boolean addContact(int uid1, int uid2, String type) {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO contacts (passenger_id1, passenger_id2, relationship_type) VALUES (?,?,?)")) {
                ps.setInt(1, uid1); ps.setInt(2, uid2); ps.setString(3, type);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) { return false; }
        }

        @Override
        public int generateTickets(String start, String end) {
            int count = 0;
            DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy.MM.dd");
            String sql = "INSERT INTO ticket (flight_id, flight_date, business_price, business_remain, economy_price, economy_remain) " +
                    "SELECT id, ?, 2000, 20, 1000, 100 FROM flight ON CONFLICT DO NOTHING";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                for (LocalDate d = LocalDate.parse(start, f); !d.isAfter(LocalDate.parse(end, f)); d = d.plusDays(1)) {
                    ps.setDate(1, Date.valueOf(d)); count += ps.executeUpdate();
                }
            } catch (SQLException e) { e.printStackTrace(); }
            return count;
        }

        @Override
        public List<Object[]> searchTicketsForGUI(String dep, String arr, String date, String al, String dt, String at) {
            List<Object[]> res = new ArrayList<>();
            String sql = "SELECT t.ticket_id, f.flight_number, f.scheduled_departure_time, f.scheduled_arrival_time, t.economy_price, t.business_price " +
                    "FROM ticket t JOIN flight f ON t.flight_id = f.id " +
                    "JOIN airport a_src ON f.source_iata_code = a_src.iata_code JOIN city c_src ON a_src.city_id = c_src.id " +
                    "JOIN airport a_dest ON f.dest_iata_code = a_dest.iata_code JOIN city c_dest ON a_dest.city_id = c_dest.id " +
                    "WHERE c_src.city_name = ? AND c_dest.city_name = ? AND t.flight_date = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, dep); ps.setString(2, arr); ps.setDate(3, Date.valueOf(date.replace('.', '-')));
                ResultSet rs = ps.executeQuery();
                while (rs.next()) res.add(new Object[]{ rs.getInt(1), rs.getString(2), rs.getTime(3), rs.getTime(4), rs.getDouble(5), rs.getDouble(6) });
            } catch (SQLException e) { e.printStackTrace(); }
            return res;
        }

        @Override
        public boolean bookTicket(int pId, int tId, String cabin) {
            try {
                con.setAutoCommit(false);
                String col = cabin.equalsIgnoreCase("Economy") ? "economy_remain" : "business_remain";
                try (PreparedStatement ps1 = con.prepareStatement("UPDATE ticket SET " + col + " = " + col + " - 1 WHERE ticket_id = ? AND " + col + " > 0")) {
                    ps1.setInt(1, tId); if (ps1.executeUpdate() == 0) { con.rollback(); return false; }
                }
                try (PreparedStatement ps2 = con.prepareStatement("INSERT INTO ticket_order (passenger_id, ticket_id, cabin_class) VALUES (?,?,?)")) {
                    ps2.setInt(1, pId); ps2.setInt(2, tId); ps2.setString(3, cabin); ps2.executeUpdate();
                }
                con.commit(); return true;
            } catch (Exception e) { try { con.rollback(); } catch (Exception ex) {} return false; }
            finally { try { con.setAutoCommit(true); } catch (Exception e) {} }
        }

        @Override
        public List<Object[]> searchOrdersForGUI(int pId, int perm) {
            List<Object[]> res = new ArrayList<>();
            String sql = "SELECT o.order_id, f.flight_number, t.flight_date, o.cabin_class, p.name FROM ticket_order o " +
                    "JOIN ticket t ON o.ticket_id = t.ticket_id JOIN flight f ON t.flight_id = f.id JOIN passenger p ON o.passenger_id = p.id";
            if (perm <= 1) sql += " WHERE o.passenger_id = ? OR o.order_id IN (SELECT order_id FROM ticket_order WHERE passenger_id IN (SELECT id FROM passenger WHERE id=?))";
            // 简化逻辑：这里主要演示。实际中可能需要更复杂的关联。
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                if (perm <= 1) { ps.setInt(1, pId); ps.setInt(2, pId); }
                ResultSet rs = ps.executeQuery();
                while (rs.next()) res.add(new Object[]{ rs.getInt(1), rs.getString(2), rs.getDate(3), rs.getString(4), rs.getString(5) });
            } catch (SQLException e) { e.printStackTrace(); }
            return res;
        }

        @Override
        public boolean deleteOrder(int oId, int perm, int pId) {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM ticket_order WHERE order_id = ?")) {
                ps.setInt(1, oId); return ps.executeUpdate() > 0;
            } catch (SQLException e) { return false; }
        }

        // 兼容方法...
        public int addOneMovie(String s) {return 0;}
        public String allContinentNames() {return null;}
        public String continentsWithCountryCount() {return null;}
        public String FullInformationOfMoviesRuntime(int i, int j) {return null;}
        public String findMovieById(int i) {return null;}
    }