package com.example.Server;

import com.alibaba.fastjson.JSONObject;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/relay_message/{home_name}")
public class RelayMessage {

    private static Map<String, LinkedList<RelayMessage>> clients = new ConcurrentHashMap<String, LinkedList<RelayMessage>>();
    private Session session;
    private String home_name;

    private final int MESSAGE_INFO = 1;
    private final int MESSAGE_WARNING = 2;
    private final int MESSAGE_ERROR = 3;

    @OnOpen
    public void onOpen(@PathParam("home_name") String home_name, Session session) throws IOException {

        this.home_name = home_name;
        this.session = session;

        LinkedList<RelayMessage> client = clients.get(home_name);
        if (client == null) {
            clients.put(home_name, new LinkedList<RelayMessage>());
            client = clients.get(home_name);
        }
        client.add(this);

        System.out.println("已连接");
    }


    @OnClose
    public void onClose() throws IOException {

        LinkedList<RelayMessage> clientList = clients.get(home_name);
        clientList.remove(this);

        //如果房间为空 则删除房间
        if (clientList.isEmpty()) {
            clients.remove(home_name);
            Server.inf.remove(home_name);
        }
    }


    @OnMessage
    public void onMessage(String message) {
        sendMessageTo(message, home_name);
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
            LinkedList<RelayMessage> toList = clients.get(To);
            for (RelayMessage item : toList) {
                if (item.home_name.equals(To))
                    if (!item.equals(this))
                        item.session.getAsyncRemote().sendText(message);
            }
        } catch (Exception ex) {
            sendMessage(ex.getMessage(), MESSAGE_ERROR);
        }
    }


    public static synchronized Map<String, LinkedList<RelayMessage>> getClients() {
        return clients;
    }

}
