import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LoggerMsg {

	SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd a HH:mm:ss");
	FileWriter writer;

	public LoggerMsg(String path) { // 로거를 실행할때 path를 문자형태로 넣겠다 //로거 실행은 메인이 한다
		try {
			writer = new FileWriter(path, true); // 같은 파일이 있어 ? 있으면 이어쓰기
		} catch (IOException e) {

			e.printStackTrace();
		}
	}

	// 로그기록
	public void log(String msg) {
		System.out.println("로그 함수 : "+msg);
		try {
			writer.write(msg); // FileWriter에 접근해서 msg를 쓰겠다
			writer.write("\t\t" + sdf.format(new Date()) + "\r\n");
			// 오늘 날짜를 특정 형태로 표시한다//format 특정형태로 표시
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void close() {

		try {
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
