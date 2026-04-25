import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        DatabaseManipulation dm = new DatabaseManipulation();
        dm.openDatasource();
        Scanner sc = new Scanner(System.in);

        int login_mode = 0;
        int permission = 0;
        String mobile_phone = null;
        String password = null;
        while(true){
            if(login_mode==0) {
                System.out.println("\nPlease enter your mobile phone");
                System.out.println("(Haven't registered? Enter \"visitor\" to use basic functions)");
                mobile_phone = sc.next();
                if(mobile_phone.equals("visitor")){
                    break;
                }
                else{
                    boolean valid_mp = true;
                    for(int i=0;i<mobile_phone.length();i++) {
                        if (mobile_phone.charAt(i) < '0' || mobile_phone.charAt(i) > '9') {
                            valid_mp = false;
                            break;
                        }
                    }
                    if(!valid_mp){
                        System.out.println("\nInvalid mobile phone");
                        continue;
                    }
                    password = dm.getPassword(mobile_phone);
                    if(password==null){
                        System.out.println("\nLogin succeed!");
                        permission= dm.getPermission(mobile_phone);
                        break;
                    } else if (password.equals("#")) {
                        System.out.println("\nUnregistered mobile phone");
                        continue;
                    }
                    else {
                        login_mode=1;continue;
                    }
                }
            } else if (login_mode==1) {
                System.out.println("\nPlease enter the password");
                System.out.println("(Change a mobile phone? Enter \"back\")");
                String ipt = sc.next();
                if(ipt.equals("back")){
                    login_mode=0;continue;
                }
                if(ipt.equals(password)){
                    System.out.println("\nLogin succeed!");
                    permission= dm.getPermission(mobile_phone);
                    break;
                }
                else{
                    System.out.println("\nWrong password");
                    continue;
                }
            }
            /*
            else if (login_mode==2) {
                System.out.println("Please enter your mobile phone");
            } else if (login_mode==3) {
                System.out.println("Please set your password");
                System.out.println("--Your password must be consist of alphabets and digits");
                System.out.println("--Length of your password must not exceed 20");
                String ipt = sc.next();
                boolean valid_p = true;
                if(ipt.length()>20){
                    valid_p = false;
                }else {
                    for (int i = 0; i < ipt.length(); i++) {
                        if (!Character.isLetterOrDigit(ipt.charAt(i))) {
                            valid_p = false;
                            break;
                        }
                    }
                }
                if(!valid_p){
                    System.out.println("\nInvalid password");
                    continue;
                }
            }
            */
        }

        int id = dm.getId(mobile_phone);

        while (true) {
            System.out.println("\nPlease select an operation:");
            System.out.println("1. Generate tickets.");
            System.out.println("2. Search Flights.");
            System.out.println("3. Manage orders.");
            System.out.println("4. Change password.");
            System.out.println("5. Exit.");
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
                if(permission==0){
                    System.out.println("Visitor cannot access order");
                    continue;
                }
                System.out.println("1. Search Orders  2. Book Ticket  3. Delete Order");
                int sub = sc.nextInt();
                if (sub == 1) { dm.searchOrders(id,permission); }
                else if (sub == 2) { System.out.print("TicketID Class(Economy/Business): "); dm.bookTicket(id, sc.nextInt(), sc.next()); }
                else if (sub == 3) { System.out.print("Order ID: "); dm.deleteOrder(sc.nextInt(),permission,id); }
            } else if (input.equals("4")) {
                if(permission==0){
                    System.out.println("Visitor cannot reset password");
                    continue;
                }
                while (true){
                    System.out.println("Please set your password");
                    System.out.println("--Your password must be consist of alphabets and digits");
                    System.out.println("--Length of your password must not exceed 20");
                    String ipt = sc.next();
                    boolean valid_p = true;
                    if(ipt.length()>20){
                        valid_p = false;
                    }else {
                        for (int i = 0; i < ipt.length(); i++) {
                            if (!Character.isLetterOrDigit(ipt.charAt(i))) {
                                valid_p = false;
                                break;
                            }
                        }
                    }
                    if(!valid_p){
                        System.out.println("\nInvalid password");
                        continue;
                    }
                    System.out.println("\nChange password succeed!");
                    dm.updatePassword(id,ipt);
                    password=ipt;
                    break;
                }
            } else if (input.equals("5")) break;
        }
        dm.closeDatasource();
    }
}

