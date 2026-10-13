import java.sql.*;

public class BusManagementJDBC {

    static final String URL =
        "jdbc:oracle:thin:@localhost:1521:XE";

    static final String USER = "25BCE1444@ORCL";
    static final String PASSWORD = "25bce1444";

    public static void main(String[] args) {

        try {

            Class.forName("oracle.jdbc.OracleDriver");

            Connection con = DriverManager.getConnection(
                URL,
                USER,
                PASSWORD
            );

            Statement stmt = con.createStatement();

            ResultSet rs = stmt.executeQuery(
                "SELECT BUS_ID, BUS_NUMBER, ROUTE, DRIVER_NAME, CAPACITY, STATUS " +
                "FROM BUS"
            );

            System.out.println("BUS MANAGEMENT SYSTEM");
            System.out.println("----------------------");

            while (rs.next()) {

                System.out.println(
                    rs.getInt("BUS_ID") + " | " +
                    rs.getString("BUS_NUMBER") + " | " +
                    rs.getString("ROUTE") + " | " +
                    rs.getString("DRIVER_NAME") + " | " +
                    rs.getInt("CAPACITY") + " | " +
                    rs.getString("STATUS")
                );
            }

            rs.close();
            stmt.close();
            con.close();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}