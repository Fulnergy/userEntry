import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        DatabaseManipulation dm = new DatabaseManipulation();
        dm.openDatasource();
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\nPlease select an operation:");
            System.out.println("1. Generate tickets.");
            System.out.println("2. Search Flights.");
            System.out.println("3. Manage orders.");
            System.out.println("4. Exit.");
            System.out.print(">> Input: ");

            String input = sc.next();
            if (input.equals("1")) {
                System.out.println("Please input the date range (e.g., 2026.04.10 2026.04.16):");
                System.out.println(dm.generateTickets(sc.next(), sc.next()) + " rows generated.");
            } else if (input.equals("2")) {
                System.out.println("Input: DepCity ArrCity Date(YYYY.MM.DD)");
                String dep = sc.next(); String arr = sc.next(); String date = sc.next();
                System.out.println("Optional: Airline DepTime ArrTime (type 'none' to skip)");
                dm.searchTickets(dep, arr, date, sc.next(), sc.next(), sc.next());
            } else if (input.equals("3")) {
                System.out.println("1. Search Orders  2. Book Ticket  3. Delete Order");
                int sub = sc.nextInt();
                if (sub == 1) { System.out.print("Passenger ID: "); dm.searchOrders(sc.nextInt()); }
                else if (sub == 2) { System.out.print("PassengerID TicketID Class(Economy/Business): "); dm.bookTicket(sc.nextInt(), sc.nextInt(), sc.next()); }
                else if (sub == 3) { System.out.print("Order ID: "); dm.deleteOrder(sc.nextInt()); }
            } else if (input.equals("4")) break;
        }
        dm.closeDatasource();
    }
}

