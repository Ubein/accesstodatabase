import java.io.IOException;
import java.sql.Connection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.Session;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author student_10
 */
public class SessionHandler {

	static Set<Session> openSessions = new HashSet<Session>();

	public static void addSession(Session session) {
		openSessions.add(session);
	}

	public static void removeSession(Session session) {
		openSessions.remove(session);
	}

	public static void sendToSession(Session session, String message) {
		try {
			session.getBasicRemote().sendText(message.toString());
		} catch (IOException ex) {
			openSessions.remove(session.getId());
			Logger.getLogger(SessionHandler.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public static void sendToallConnectedSessions(String message) {
		for (Iterator<Session> it = openSessions.iterator(); it.hasNext();) {
			Session session = it.next();
			SessionHandler.sendToSession(session, message);
		}
	}

}