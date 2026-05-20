import java.sql.*;
public class CheckBalance {
  public static void main(String[] args) throws Exception {
    Class.forName("org.sqlite.JDBC");
    try (Connection c = DriverManager.getConnection("jdbc:sqlite:C:/Users/PREC 3561/baitaplon/data/auction.db");
         Statement st = c.createStatement();
         ResultSet rs = st.executeQuery("SELECT username, role, balance FROM users ORDER BY id")) {
      while (rs.next()) {
        System.out.println(rs.getString("username") + "|" + rs.getString("role") + "|" + rs.getDouble("balance"));
      }
    }
  }
}
