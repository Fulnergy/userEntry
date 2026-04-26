import java.util.List;

public interface DataManipulation {
    void openDatasource();
    void closeDatasource();

    // 账户与权限
    String getPassword(String mobile_phone);
    int getPermission(String mobile_phone);
    int getId(String mobile_phone);
    void updatePassword(int id, String password);

    // 核心业务
    int generateTickets(String startDate, String endDate);
    List<Object[]> searchTicketsForGUI(String depCity, String arrCity, String date, String airline, String depTime, String arrTime);
    boolean bookTicket(int passengerId, int ticketId, String cabinClass);
    List<Object[]> searchOrdersForGUI(int passengerId, int permission);
    boolean deleteOrder(int orderId, int permission, int passengerId);

    // 亲属管理 
    List<Object[]> getContacts(int passengerId);
    boolean addContact(int uid1, int uid2, String type);
}