import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

class Player {
	String username;
	int win;
	int lose;

	public Player(String username, int win, int lose) {
		this.username = username;
		this.win = win;
		this.lose = lose;
	}
}

public class Server {
	private ServerSocket ss = null;
	private Socket s = null;

	int cnt = 0;
	Queue<BlueMarbleThread> player = new LinkedList<>();
	ArrayList<BlueMarbleThread> playerThreads = new ArrayList<BlueMarbleThread>();
	
	LoggerMsg loggerMsg = null;
	LocalDateTime ldt = LocalDateTime.now();
	File dir = new File("C:/bluemarble/log");

	public void start() {
		if(!dir.exists()) {
			dir.mkdirs();
			System.out.println("디렉토리 생성!!");
		}
		 
		String filename = "log_"+LocalDate.now()+".log"; 
		loggerMsg = new LoggerMsg(dir+"/"+filename);
		try {			
			ss = new ServerSocket(8888);
			System.out.println("server start");
			loggerMsg.log("server start");
			loggerMsg.close();
			
			machtingPlayer();
			while (true) {
				s = ss.accept();

				BlueMarbleThread p = new BlueMarbleThread();

				playerThreads.add(p);

				p.start();
			}
		} catch (Exception e) {
			System.out.println("[Multi Server]start() Exception 발생!!");
		}
	}

	public static void main(String[] args) {
		Server server = new Server();
		server.start();
	}

	void msgSendAll(String msg) {
		for (BlueMarbleThread ct : playerThreads) {
			ct.outMsg.println(msg);
		}
	}

	void msgSend(String msg, BlueMarbleThread thread) {
		thread.outMsg.println(msg);
	}

	void machtingPlayer() {
		Thread machting = new Thread() {
			@Override
			public void run() {
				while (true) {
					if (player.size() == 2) {
						BlueMarbleThread player1 = player.poll();
						BlueMarbleThread player2 = player.poll();
						player1.yourPlayer = player2;
						player2.yourPlayer = player1;
						String p1msg = "Start/" + player2.myPlayer.username + "/" + player2.myPlayer.win + "/"
								+ player2.myPlayer.lose + "/" + 0;
						String p2msg = "Start/" + player1.myPlayer.username + "/" + player1.myPlayer.win + "/"
								+ player1.myPlayer.lose + "/" + 1;
						msgSend(p1msg, player1);
						msgSend(p2msg, player2);
					}
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		machting.start();
	}

	class BlueMarbleThread extends Thread {
		DBConn dbConn = new DBConn();
		String msg;
		String[] rmsg;
		private BufferedReader inMsg = null;
		private PrintWriter outMsg = null;
		Player myPlayer = null;
		BlueMarbleThread yourPlayer = null;
		String ready = null;

		public void run() {
			String filename = "log_"+LocalDate.now()+".log"; 
			loggerMsg = new LoggerMsg(dir+"/"+filename);
			boolean status = true;
			System.out.println("##ChatThread start...");
			loggerMsg.log("##ChatThread start...");
			try {

				inMsg = new BufferedReader(new InputStreamReader(s.getInputStream()));
				outMsg = new PrintWriter(s.getOutputStream(), true);

				while (status) {

					msg = inMsg.readLine();
					rmsg = msg.split("/");
					String tmsg = "";

					if (msg != null) {
						System.out.println(msg);
						if (rmsg[0].equals("Login")) {
							System.out.println(Thread.currentThread());
							String sql = rmsg[1];
							dbWork(rmsg[0], sql, tmsg);	
						} else if (rmsg[0].equals("Signup")) {
							String sql = rmsg[1];
							dbWork(rmsg[0], sql, tmsg);
						} else if (rmsg[0].equals("GameResult")) {
							String winsql = "UPDATE member SET win = win+1 where id = '" + rmsg[1] + "'";
							String losesql = "UPDATE member SET lose = lose+1 where id = '" + rmsg[2] + "'";
							System.out.println("승리자:" + rmsg[1] + ",루저:" + rmsg[2]);
							loggerMsg.log("승리자:" + rmsg[1] + ",루저:" + rmsg[2]);
							dbWork(rmsg[0], winsql, tmsg);
							dbWork(rmsg[0], losesql, tmsg);
							msgSend(msg, yourPlayer);
							yourPlayer.yourPlayer = null;
							yourPlayer = null;
						} else if (rmsg[0].equals("Ready")) {
							ready = rmsg[1];
							player.offer(BlueMarbleThread.this);
						} else if (rmsg[0].equals("GameFinish")) {// SocketConnect.getOutMsg().println("Giveup/"+myName.getText().toString()+"/"+yourName.getText().toString());							
							status = false;
							loggerMsg.log("게임 종료!!");
							loggerMsg.close();
						} else if (rmsg[0].equals("ChangeTurn") || rmsg[0].equals("removeFlag")) {
							msgSend(msg, yourPlayer);
							msgSend(msg, BlueMarbleThread.this);
						} else if (rmsg[0].equals("CheckMatching")) {
							if (yourPlayer == null) {								
								status = false;
							} else {
								String winsql = "UPDATE member SET win = win+1 where id = '"
										+ yourPlayer.myPlayer.username + "'";
								String losesql = "UPDATE member SET lose = lose+1 where id = '" + myPlayer.username
										+ "'";
								System.out.println("승리자:" + yourPlayer.myPlayer.username + ",루저:" + myPlayer.username);
								loggerMsg.log("승리자:\" + yourPlayer.myPlayer.username + \",루저:\" + myPlayer.username");
								dbWork(rmsg[0], winsql, tmsg);
								dbWork(rmsg[0], losesql, tmsg);
								tmsg = "GameResult/" + yourPlayer.myPlayer.username + "/" + myPlayer.username;
								msgSend(tmsg, yourPlayer);
								yourPlayer.yourPlayer = null;
								yourPlayer = null;
								status = false;
							}
						} else if (rmsg[0].equals("FindPW")) {
							String sql = rmsg[1];
							dbWork(rmsg[0], sql, tmsg);							
						} else {							
							msgSend(msg, yourPlayer);
						}
						msg = null;
					}
				}
				if (ready != null) {
					player.poll();
				}
				playerThreads.remove(this);
				this.interrupt();
				System.out.println("##" + this.getName() + "stop!!");
				loggerMsg.log("##" + this.getName() + "stop!!");
				loggerMsg.close();
			} catch (IOException e) {
				playerThreads.remove(this);
				System.out.println("[ChatThread]run() IOException 발생!!");
				loggerMsg.log("[ChatThread]run() IOException 발생!!");
				loggerMsg.close();
			}
		}

		public void dbWork(String menu, String sql, String tmsg) {			
			String filename = "log_"+LocalDate.now()+".log"; 
			loggerMsg = new LoggerMsg(dir+"/"+filename);
			try {
				dbConn.connectDB();
				switch (menu) {

				case "Login":
					dbConn.rs = dbConn.state.executeQuery(sql);
					if (!dbConn.rs.next()) {
						tmsg = "Error";						
					} else {
						tmsg = dbConn.rs.getString(1) + "/" + dbConn.rs.getInt(2) + "/" + dbConn.rs.getInt(3);
						myPlayer = new Player(dbConn.rs.getString(1), dbConn.rs.getInt(2), dbConn.rs.getInt(3));
					}
					loggerMsg.log(myPlayer+"님 로그인.");
					loggerMsg.close();
					msgSend(tmsg, BlueMarbleThread.this);
					break;

				case "FindPW":
					dbConn.rs = dbConn.state.executeQuery(sql);
					tmsg = "";
					System.out.println(dbConn.rs);
					loggerMsg.log(dbConn.rs.getString(1)+" 패스워드 찾기 시도");
					loggerMsg.close();
					if (!dbConn.rs.next()) {
						tmsg = "FindPW/NotFound";
					} else {
						tmsg = "FindPW/" + dbConn.rs.getString(1);
						System.out.println("pw>>>>>" + tmsg);
					}					
					msgSend(tmsg, BlueMarbleThread.this);
					break;
				default:
					dbConn.state.executeUpdate(sql);
					break;
				}
				dbConn.disconnectDB();
			} catch (Exception e) {
				System.err.println(e.getMessage());
				loggerMsg.log("로그인 Exception : "+ e.getMessage());
				loggerMsg.close();
			}
		}
	}
}