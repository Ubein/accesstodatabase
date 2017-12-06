import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import com.microsoft.sqlserver.jdbc.*;

/**
 * @ServerEndpoint gives the relative name for the end point This will be
 *                 accessed via ws://localhost:8080/EchoChamber/echo Where
 *                 "localhost" is the address of the host, "EchoChamber" is the
 *                 name of the package and "echo" is the address to access this
 *                 class from the server
 */
@ServerEndpoint("/echo")
public class EchoServer {
	/**
	 * @OnOpen allows us to intercept the creation of a new session. The session
	 *         class allows us to send data to the user. In the method onOpen, we'll
	 *         let the user know that the handshake was successful.
	 */
	@OnOpen
	public void onOpen(Session session) {
		SessionHandler.addSession(session);
		SessionHandler.sendToSession(session, "MESSAGE:Connected to the server");
	}

	/**
	 * When a user sends a message to the server, this method will intercept the
	 * message and allow us to react to it. For now the message is read as a String.
	 */
	@OnMessage
	public void onMessage(String message, Session session) {
		System.out.println(message);
		if (message.equals("Show tables")) {
			getDatabaseTables(session);
		} else {
			writeResponseToSQLQuery(session, message);
		}
	}

	/**
	 * The user closes the connection.
	 * 
	 * Note: you can't send messages to the client from this method
	 */
	@OnClose
	public void onClose(Session session) {
		SessionHandler.removeSession(session);
	}

	public void getDatabaseTables(Session session) {
		String url = "jdbc:sqlserver://mojabaza.database.windows.net:1433;database=mySampleDatabase;user=Ubein@mojabaza;password=Roaming1;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;";
		Connection connection = null;
		int Liczba = 0;
		try {
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
			connection = DriverManager.getConnection(url);
			String schema = connection.getSchema();
			String selectSql = String.format("select name from sys.tables");
			try (Statement statement = connection.createStatement();
					ResultSet resultSet = statement.executeQuery(selectSql)) {
				while (resultSet.next()) {
					SessionHandler.sendToSession(session, "TABLE:" + resultSet.getString(1));
				}
				connection.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void writeResponseToSQLQuery(Session session, String selectSql) {
		String result;
		String columnsname = "";
		String[] parts = selectSql.split(" ");
		Connection connection = null;
		String url = "jdbc:sqlserver://mojabaza.database.windows.net:1433;database=mySampleDatabase;user=Ubein@mojabaza;password=Roaming1;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;";
		if (parts[0].equalsIgnoreCase("select")) {
			try {
				Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
				connection = DriverManager.getConnection(url);
				String schema = connection.getSchema();
				try (Statement statement = connection.createStatement();
						ResultSet resultSet = statement.executeQuery(selectSql)) {
					ResultSetMetaData rsmd = resultSet.getMetaData();
					int columnsNumber = rsmd.getColumnCount();
					result = "<table id=\"example\" class=\"table table-bordered\" cellspacing=\"0\" style=\"width:100%\">";
					result += "<thead> <tr>";
					for (int i = 1; i <= columnsNumber; i++) {
						columnsname = "MESSAGE:COLUMNSNAME:" + rsmd.getColumnName(i);
						SessionHandler.sendToSession(session, columnsname);
						result += "<th>" + rsmd.getColumnName(i) + "</th>";
					}
					result += "</tr> </thead><tbody> ";
					while (resultSet.next()) {
						result += "<tr>";
						for (int i = 1; i <= columnsNumber; i++)
							result += "<td>" + resultSet.getString(i) + "</td>";
						result += "</tr>";
					}
					result += "</tbody> </table>";
					SessionHandler.sendToSession(session, result);
					connection.close();
				}
			} catch (SQLServerException ex) {
				SessionHandler.sendToSession(session, "ERROR:" + ex.getMessage());
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {

			try {
				Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
				connection = DriverManager.getConnection(url);
				String schema = connection.getSchema();
				try (Statement statement = connection.createStatement();
						ResultSet resultSet = statement.executeQuery(selectSql)) {
					connection.close();

				}

			} catch (SQLServerException ex) {
				String message = ex.getMessage();
				if (!message.equals("The statement did not return a result set.")) {
					SessionHandler.sendToSession(session, "ERROR:" + ex.getMessage());
				} else {
					SessionHandler.sendToSession(session, "MESSAGE:Query successful");
					SessionHandler.sendToallConnectedSessions("MESSAGE:Refresh");
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}
}