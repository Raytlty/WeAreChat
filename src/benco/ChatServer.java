package benco;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.websocket.*;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.IOException;
/**
 * 聊天服务器类
 * @author Benco
 *
 */
@ServerEndpoint("/websocket")
public class ChatServer {
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");	// 日期格式化
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private  Session _session;
	private  String nickname = null;
	private DataBase db = new DataBase(logger);
	private static int user_id = 0;
	private static ConcurrentHashMap<String,ChatServer> Container = new ConcurrentHashMap<String,ChatServer>();
	private static ConcurrentHashMap<Session,Integer> Session_Container = new ConcurrentHashMap<Session,Integer> ();

	@OnOpen
	public void open(Session session , EndpointConfig config) {
		_session = session;
		/*user_id = _session.getId();
		Session_Container.put(_session,user_id);*/
		logger.info(user_id + " is Connected..");
	}
	/**
	 * 接受客户端的消息，并把消息发送给所有连接的会话
	 * @param message 客户端发来的消息
	 * @param session 客户端的会话
	 */
	@OnMessage
	public void getMessage(String message, Session session) throws IOException {
		logger.info(session.getId()+" Received: "+message);
		parseCmd(message);
		/*JSONObject jsonObject = new JSONObject(message);
		jsonObject.put("date", DATE_FORMAT.format(new Date()));
		for (Session openSession : session.getOpenSessions()) {
			jsonObject.put("isSelf", openSession.equals(session));
			openSession.getAsyncRemote().sendText(jsonObject.toString());
		}*/
	}

	@OnClose
	public void close(Session session , CloseReason closeReason) {
		logger.info(String.format("Session %s closed because of %s", session.getId(), closeReason));
		//Container.remove(nickname);
	}

	@OnError
	public void error(Session session , Throwable t) {
		logger.info(String.format("Session %s error because of %s", session.getId(), t));
	}
	private void send(Session session , String msg) throws IOException{
		session.getBasicRemote().sendText(msg);
	}
	private void parseCmd(String message) throws IOException {
		JSONTokener json_tokener = new JSONTokener(message);
		JSONObject jsonObject = (JSONObject) json_tokener.nextValue();

		String type = jsonObject.getString("type");
		switch (type) {
			case "login" :
				handleLogin(jsonObject);
				break;
			case "register":
				handleRegister(jsonObject);
				break;
			case "sendmsg":
				handleSend(jsonObject);
				break;
			case "whisper" :
				handleWhisper(jsonObject);
				break;
		}
	}
	private void handleLogin(JSONObject jsonObject) throws IOException{
		String nickname = jsonObject.getString("nickname");
		String passwd = jsonObject.getString("passwd");
		Boolean sign= db.Login(nickname, passwd);
		JSONObject ret = new JSONObject().put("type", "login")
				.put("status", sign.toString());
		//System.out.println(Container);
		if (sign && !Container.containsKey(nickname)) {
			this.nickname = nickname;
			ret.put("user_id", this.user_id)
				.put("nickname", nickname)
				.put("passwd", passwd)
				.put("msg", "Login succeed,welcome back," + nickname);
			Container.put(nickname, this);
			Session_Container.put(_session,user_id);
			System.out.println(Container.keys());
			user_id++;
		}
		else {
			ret.put("msg", "Sorry login failed");
		}
		send(_session , ret.toString());
	}
	private void handleRegister(JSONObject jsonObject) throws IOException {
		String nickname =jsonObject.getString("nickname");
		String passwd = jsonObject.getString("passwd");
		Boolean sign = db.Register(nickname, passwd);
		JSONObject ret = new JSONObject().put("type", "register")
				.put("status", sign.toString())
				.put("nickname", nickname)
				.put("passwd", passwd);
		if(sign) {
			ret.put("msg", "Register succeed");
		}
		else {
			ret.put("msg", "Sorry register failed");
		}
		send(_session, ret.toString());
	}
	private void handleSend(JSONObject jsonObject)  throws IOException{
		String content = jsonObject.getString("content");
		JSONObject ret = new JSONObject()
				.put("type", "sendmsg")
				.put("nickname", nickname)
				.put("content", content)
				.put("date", DATE_FORMAT.format(new Date()));
		for (Session openSession : _session.getOpenSessions()) {
			ret.put("session",openSession);
			ret.put("isSelf", openSession.equals(_session));
			ret.put("user_id",Session_Container.get(_session).intValue());
			System.out.println(openSession);
			send(openSession,ret.toString());
		}
	}
	private void handleWhisper(JSONObject jsonObject) {
		String content = jsonObject.getString("content");
		String username = jsonObject.getString("username");
		ChatServer items = Container.get(username);
		System.out.println(items);
		try {
			Session openSession = items._session;
			JSONObject ret = new JSONObject()
					.put("type","sendmsg")
					.put("nickname", username)
					.put("content", content)
					.put("date", DATE_FORMAT.format(new Date()))
					.put("session",openSession)
					.put("isSelf", openSession.equals(_session))
					.put("user_id",Session_Container.get(_session).intValue());
			send(openSession,ret.toString());
			JSONObject res = new JSONObject()
					.put("type","sendmsg")
					.put("nickname", nickname)
					.put("content", content)
					.put("date", DATE_FORMAT.format(new Date()))
					.put("session",_session)
					.put("isSelf", _session.equals(_session))
					.put("user_id",Session_Container.get(_session).intValue());
			send(_session,res.toString());
		}
		catch (Exception e) {}
	}
}