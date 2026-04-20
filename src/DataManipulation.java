import java.util.List;

public interface DataManipulation {
    void openDatasource();
    void closeDatasource();

    // Task 4.1: 生成机票
    int generateTickets(String startDate, String endDate);

    // Task 4.2: 搜索机票
    void searchTickets(String depCity, String arrCity, String date, String airline, String depTime, String arrTime);

    // Task 4.3: 订票
    boolean bookTicket(int passengerId, int ticketId, String cabinClass);

    // Task 4.4: 订单管理
    void searchOrders(int passengerId);
    boolean deleteOrder(int orderId);

    // 兼容之前作业的方法（留空即可）
    int addOneMovie(String str);
    String allContinentNames();
    String continentsWithCountryCount();
    String FullInformationOfMoviesRuntime(int min, int max);
    String findMovieById(int id);
}