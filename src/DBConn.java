
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConn{
	public static Connection getConnection() throws SQLException{
		String url = "jdbc:mysql://localhost:3306/bluemable";
		String id = "root";
		String password = "root";
		Connection con = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
				System.out.println("드라이버 적재 성공");
			con = DriverManager.getConnection(url,id,password);
				System.out.println("데이터베이스 연결 성공");
		}catch(ClassNotFoundException e) {
			System.out.println("드라이버를 찾을수 없습니다.");
		}catch(SQLException e) {
			System.out.println("연결에 실패하였습니다.");
		}
		return con;
	}
	
}


 
