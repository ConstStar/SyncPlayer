package cn.xiaoxiaoge.Server;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    public static class Inf {
        public String url;
        public String room;
//        public int time;
//        public int changeTime;
//        public int state;

        public Inf(String url, String room) {
            this.url = url;
            this.room = room;
        }

        //状态
        public static final int EVENT_PLAY = 0;       //播放
        public static final int EVENT_PAUSE = 1;      //暂停
        public static final int EVENT_LOAD = 2;       //加载
    }

    public static HashMap<String, Inf> inf = new HashMap<String, Inf>();
    public static Map<String, LinkedList<RelayMessage>> clients = new ConcurrentHashMap<String, LinkedList<RelayMessage>>();


    Server() {

        //定时清空空闲房间
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {

                ArrayList <String> delRoomList=new ArrayList<String>();

                for (String room : clients.keySet()) {
                    //如果房间为空 则删除房间
                    if (clients.get(room).isEmpty()) {
                        delRoomList.add(room);
                    }
                }

                for (String room : delRoomList) {
                    clients.remove(room);
                    inf.remove(room);
                }

            }
        }, 0, 3 * 60 * 1000);
    }
}
