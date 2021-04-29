
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConn{
	String url = "jdbc:mysql://localhost/bluemable";
    String id = "root";
    String password = "root";

    ResultSet rs = null;
    Connection con = null;
    Statement state = null;
    public void connectDB() throws Exception {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            System.out.println("드라이버 적재 성공");
            con = DriverManager.getConnection(url, id, password);
            System.out.println("데이터베이스 연결 성공");
            state = con.createStatement();
        } catch (ClassNotFoundException e) {
            System.out.println("드라이버 로드 실패");
        } catch (SQLException e) {
            System.out.println("연결에 실패하였습니다.");
        }
    }

    public void disconnectDB() {
        try {
            if(rs!=null) rs.close();
            if(state!=null)state.close();
            if(con!=null)con.close();
            System.out.println("데이터베이스 연결종료 성공");
        } catch (Exception e) {
            System.out.println("데이터베이스 연결종료 실패");
        }
    }
	
}


 
