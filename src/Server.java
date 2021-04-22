import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

	public void start() {
		try {

			ss = new ServerSocket(8888);
			System.out.println("server start");
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

		String msg;
		String[] rmsg;
		Connection con = null;
		PreparedStatement psmt = null;
		ResultSet rs = null;
		String ready = null;
		private BufferedReader inMsg = null;
		private PrintWriter outMsg = null;
		Player myPlayer = null;
		BlueMarbleThread yourPlayer = null;

		public void run() {

			boolean status = true;
			System.out.println("##ChatThread start...");
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
							try {
								con = DBConn.getConnection();
							} catch (SQLException e) {
								e.printStackTrace();
							}

							try {
								psmt = con.prepareStatement(sql);
								rs = psmt.executeQuery();
								System.out.println(rs);
								if (!rs.next()) {
									tmsg = "Error";
									msgSend(tmsg, BlueMarbleThread.this);
								} else {
									tmsg = rs.getString(1) + "/" + rs.getInt(2) + "/" + rs.getInt(3);
									myPlayer = new Player(rs.getString(1), rs.getInt(2), rs.getInt(3));
									msgSend(tmsg, BlueMarbleThread.this);
								}
							} catch (SQLException ex) {
								System.err.println(ex.getMessage());
							}

						} else if (rmsg[0].equals("Signup")) {
							String sql = rmsg[1];
							try {
								con = DBConn.getConnection();
								Statement state = con.createStatement();
								int rs = state.executeUpdate(sql);

							} catch (SQLException e) {
								e.printStackTrace();
							}
						} else if (rmsg[0].equals("GameResult")) {
							String winsql = "UPDATE member SET win = win+1 where id = '" + rmsg[1] + "'";
							String losesql = "UPDATE member SET lose = lose+1 where id = '" + rmsg[2] + "'";
							System.out.println("승리자:" + rmsg[1] + ",루저:" + rmsg[2]);
							try {
								con = DBConn.getConnection();
								Statement state = con.createStatement();
								state.executeUpdate(winsql);
								state.executeUpdate(losesql);
							} catch (SQLException e) {
								e.printStackTrace();
							}
							msgSend(msg, yourPlayer);
							yourPlayer.yourPlayer = null;
							yourPlayer = null;
						} else if (rmsg[0].equals("Ready")) {
							ready = rmsg[1];
							player.offer(BlueMarbleThread.this);
						} else if (rmsg[0].equals("GameFinish")) {// SocketConnect.getOutMsg().println("Giveup/"+myName.getText().toString()+"/"+yourName.getText().toString());
							if (ready != null) {
								player.poll();
							}
							status = false;
						} else if (rmsg[0].equals("ChangeTurn") || rmsg[0].equals("removeFlag")) {
							msgSend(msg, yourPlayer);
							msgSend(msg, BlueMarbleThread.this);
						} else if (rmsg[0].equals("CheckMatching")) {
							if (yourPlayer == null) {
								if (ready != null) {
									player.poll();
								}
								status = false;
							} else {
								String winsql = "UPDATE member SET win = win+1 where id = '"+ yourPlayer.myPlayer.username + "'";
								String losesql = "UPDATE member SET lose = lose+1 where id = '" + myPlayer.username+ "'";
								System.out.println("승리자:" + yourPlayer.myPlayer.username + ",루저:" + myPlayer.username);
								try {
									con = DBConn.getConnection();
									Statement state = con.createStatement();
									state.executeUpdate(winsql);
									state.executeUpdate(losesql);
								} catch (SQLException e) {
									e.printStackTrace();
								}
								tmsg = "GameResult/" + yourPlayer.myPlayer.username + "/" + myPlayer.username;
								msgSend(tmsg, yourPlayer);
								yourPlayer.yourPlayer = null;
								yourPlayer = null;
								status = false;
							}
						} else if (rmsg[0].equals("FindPW")) {
							String sql = rmsg[1];
							try {
								con = DBConn.getConnection();
							} catch (SQLException e) {
								e.printStackTrace();
							}

							try {
								psmt = con.prepareStatement(sql);
								rs = psmt.executeQuery();
								tmsg = "";
								System.out.println(rs);
								if (!rs.next()) {
									tmsg = "FindPW/NotFound";
								} else {
									tmsg = "FindPW/" + rs.getString(1);
									// player.offer(new Player(rs.getString(1), rs.getInt(2), rs.getInt(3)));
									System.out.println("pw>>>>>" + tmsg);
								}
								msgSend(tmsg, BlueMarbleThread.this);
							} catch (SQLException ex) {
								System.err.println(ex.getMessage());
							}
						} else {
							System.out.println("1: " + yourPlayer);
							msgSend(msg, yourPlayer);
						}
						msg = null;
					}
				}

				con.close();
				psmt.close();
				rs.close();
				playerThreads.remove(this);
				this.interrupt();

				System.out.println("##" + this.getName() + "stop!!");
			} catch (IOException e) {

				System.out.println("[ChatThread]run() IOException 발생!!");
			} catch (SQLException e) {
			}
		}
	}
}