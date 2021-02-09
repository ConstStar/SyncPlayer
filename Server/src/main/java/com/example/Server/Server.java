package com.example.Server;

import java.util.HashMap;
import java.util.Map;

public class Server {

    public static class Inf {
        public String url;
        public String home;
//        public int time;
//        public int changeTime;
//        public int state;

        public Inf(String url, String home) {
            this.url = url;
            this.home = home;
        }

        //状态
        public static final int EVENT_PLAY = 0;       //播放
        public static final int EVENT_PAUSE = 1;      //暂停
        public static final int EVENT_LOAD = 2;       //加载
    }

    public static HashMap<String, Inf> inf = new HashMap<String, Inf>();
}
