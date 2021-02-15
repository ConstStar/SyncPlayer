package cn.xiaoxiaoge.Server;

import com.alibaba.fastjson.JSONObject;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/relay_message/{rome_name}")
public class RelayMessage {
    private Session session;
    private String rome_name;

    private final int MESSAGE_INFO = 1;
    private final int MESSAGE_WARNING = 2;
    private final int MESSAGE_ERROR = 3;

    @OnOpen
    public void onOpen(@PathParam("rome_name") String rome_name, Session session) throws IOException {

        this.rome_name = rome_name;
        this.session = session;

        LinkedList<RelayMessage> client = Server.clients.get(rome_name);
        if (client == null) {
            Server.clients.put(rome_name, new LinkedList<RelayMessage>());
            client = Server.clients.get(rome_name);
        }
        client.add(this);

        System.out.println("已连接");
    }


    @OnClose
    public void onClose() throws IOException {

        LinkedList<RelayMessage> clientList = Server.clients.get(rome_name);
        clientList.remove(this);
    }


    @OnMessage
    public void onMessage(String message) {
        sendMessageTo(message, rome_name);
    }


    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

    //给自己发消息
    public void sendMessage(String messge, int type) {
        JSONObject obj = new JSONObject();
        obj.put("type", type);
        obj.put("message", messge);
        this.session.getAsyncRemote().sendText(obj.toString());
    }

    //给别人发消息
    public void sendMessageTo(String message, String To) {
        try {
            LinkedList<RelayMessage> toList = Server.clients.get(To);
            for (RelayMessage item : toList) {
                if (item.rome_name.equals(To))
                    if (!item.equals(this))
                        item.session.getAsyncRemote().sendText(message);
            }
        } catch (Exception ex) {
            sendMessage(ex.getMessage(), MESSAGE_ERROR);
        }
    }
}
