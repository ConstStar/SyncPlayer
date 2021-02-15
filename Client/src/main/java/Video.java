import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import uk.co.caprica.vlcj.media.MediaRef;
import uk.co.caprica.vlcj.media.TrackType;
import uk.co.caprica.vlcj.player.base.*;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;

public class Video {
    String roomId;
    String url;
    String roomName;//房间名称

    JFrame videoFrame; //JFrame 实例
    Container containerPane;//界面容器

    EmbeddedMediaPlayerComponent player; //视频播放器
    ControlsApi playerControls; //视频控制器
    EventApi playerEvents; //视频控制器
    MediaApi playerMedia;  //视频媒体
    StatusApi playerStatus;  //播放器状态
    VideoApi playerVideo;  //视频
    AudioApi playerAudio;  //声音

    JPanel videoPane;//视频容器
    JPanel panel;   //控制区域容器
    //    JPanel progressPanel;   //进度条容器
    JProgressBar progress;  //进度条
    JSlider volumeSlider;   //设置声音
    JLabel volumeLabel;     //声音文本
    JLabel labelTime;       //显示进度

    Websock websock;


    public static void outInf(String message) {
        System.out.println(message);
    }

    class Websock extends WebSocketClient {

        boolean isRecvTime = true; //是否可以接受时间调整
        public boolean isRecvOneTime = true;//是否接受到第一个时间

        //事件
        public static final int EVENT_PLAY = 0;       //播放
        public static final int EVENT_PAUSE = 1;      //暂停
        public static final int EVENT_STOP = 2;       //停止
        public static final int EVENT_LOAD = 3;       //加载
        public static final int EVENT_SEEK = 4;       //调整时间

        //服务器消息
        private final int MESSAGE_INFO = 1;
        private final int MESSAGE_WARNING = 2;
        private final int MESSAGE_ERROR = 3;

        //客户端消息
        public final int MESSAGE_EVENT = 11; //事件


        public Websock(String serverUri) {
            super(URI.create(serverUri));
            this.connect();
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {
            System.out.println("onOpen");

            //两秒内未收到第一次接收的时间 将视为自己为第一个客户端
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    isRecvOneTime = false;
                }
            }, 2*1000);
        }

        @Override
        public synchronized void onMessage(String s) {
            //System.out.println("onMessage " + s);

            JSONObject json = JSONObject.parseObject(s);

            switch (json.getInteger("type")) {

                //服务器消息
                case MESSAGE_WARNING:
                case MESSAGE_ERROR:
                case MESSAGE_INFO: {
                    outInf(json.getString("message"));
                    break;
                }

                //客户端消息
                case MESSAGE_EVENT: {
                    Integer event = json.getInteger("event");
                    switch (event) {
                        case EVENT_SEEK:
                            long time = playerStatus.time();

                            boolean isChangeTime = json.getBoolean("isChangeTime");
                            int retime = json.getInteger("time");
                            int timeAbs = Math.abs(retime - (int) time);

                            //第一次接收时间
                            if (isRecvOneTime) {
                                playerControls.setTime(retime);
                                isRecvOneTime = false;
                            }
                            //时间被调整
                            else if (isChangeTime) {
                                //调整时间大小大于1秒才进行调整
                                if (timeAbs > 1*1000)
                                    playerControls.setTime(retime);
                            } else {
                                //如果正在发送时间消息 而且收到的并非强制调整时间 则不作处理
                                if (!isRecvTime) {
                                    break;
                                }

                                if (timeAbs > 2 * 60 * 1000) {
                                    //延迟大于2分钟直接修改播放进度
                                    playerControls.setTime(retime);
                                } else if (timeAbs > 1 * 1000) {
                                    //延迟大于1秒自动提速
                                    playerControls.setRate(1.1f);
                                } else {
                                    playerControls.setRate(1.0f);
                                }
                            }
                            break;
                        case EVENT_PLAY:
                            playerControls.setPause(false);
                            break;
                        case EVENT_PAUSE:
                            playerControls.setPause(true);
                            break;
                        case EVENT_STOP:
                            playerControls.stop();
                            break;
                    }
                    break;
                }
            }

        }

        @Override
        public void onClose(int i, String s, boolean b) {
            System.out.println("onClose " + s);
        }

        @Override
        public void onError(Exception e) {
            System.out.println("onError " + e.getMessage());
        }

        //向其他客户端 发送消息
        public synchronized void sendMessage(String message) {
//            outInf("sendMessage");

            try {
                this.send(message);
            } catch (Exception ex) {
                outInf("发送错误 " + ex.getMessage());
            }

        }

        public synchronized void putEvent(int event) {
            //未接收到第一次时间 禁止发送
            if (isRecvOneTime)
                return;

            try {
                JSONObject json = new JSONObject();
                json.put("type", MESSAGE_EVENT);
                json.put("event", event);

                sendMessage(json.toString());

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }

        //更新播放时间
        public synchronized void putTime(boolean isChangeTime) {

            //未接收到第一次时间 禁止发送
            if (isRecvOneTime)
                return;

            isRecvTime = false;
            try {
                long time = playerStatus.time();
                JSONObject json = new JSONObject();
                json.put("type", MESSAGE_EVENT);
                json.put("event", EVENT_SEEK);
                json.put("time", time);
                json.put("isChangeTime", isChangeTime);

                sendMessage(json.toString());

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
            isRecvTime = true;
        }

    }

    public EmbeddedMediaPlayerComponent getPlayer() {
        return player;
    }

    public ControlsApi getPlayerControls() {
        return playerControls;
    }

    public EventApi getPlayerEvents() {
        return playerEvents;
    }

    //初始化 界面控制内容
    private void initFrameControl() {
        //实例化控制区域容器
        panel = new JPanel();
        panel.setLayout(new BorderLayout(0, 0));
        //添加进度条
        progress = new JProgressBar();
        panel.add(progress, BorderLayout.CENTER);
        //点击进度条调整视屏播放进度
        progress.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int x = e.getX();
                float position = (float) x / progress.getWidth();
                int time = (int) (position * playerMedia.info().duration());

                //调整视频进度
                playerControls.setPosition(position);

                //调整进度条进度
                progress.setValue(time);

                //发送调整进度请求
                websock.putTime(true);
            }
        });
        progress.setStringPainted(true);

        //控制按钮
        JButton btnForward = new JButton("前进");
        btnForward.addActionListener(e -> {
            long time = playerStatus.time() + 10 * 1000;
            //调整进度条进度
            playerControls.setTime(time);
            //发送调整进度请求
            websock.putTime(true);
        });

        JButton btnBack = new JButton("后退");
        btnBack.addActionListener(e -> {
            long time = playerStatus.time() - 10 * 1000;
            //调整进度条进度
            playerControls.setTime(time);
            //发送调整进度请求
            websock.putTime(true);
        });

        JButton btnPlay = new JButton("播放/暂停");
        btnPlay.addActionListener(e -> {
            if (playerStatus.isPlaying())
                playerControls.pause();
            else
                playerControls.play();
        });

        JButton btnStop = new JButton("停止");
        btnStop.addActionListener(e -> {
            playerControls.stop();
        });

        //设置音量
        volumeSlider = new JSlider();
        volumeSlider.setOrientation(JSlider.HORIZONTAL);
        volumeSlider.setMinimum(LibVlcConst.MIN_VOLUME);
        volumeSlider.setMaximum(LibVlcConst.MAX_VOLUME);
        volumeSlider.setPreferredSize(new Dimension(100, 40));
        volumeSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                playerAudio.setVolume(volumeSlider.getValue());
            }
        });
        volumeLabel = new JLabel();

        //显示播放时长区域
        labelTime = new JLabel();

        JPanel bottomPane = new JPanel();
//        bottomPane.setLayout(new BorderLayout(0,0));
        bottomPane.add(btnBack, BorderLayout.CENTER);
        bottomPane.add(btnPlay, BorderLayout.CENTER);
        bottomPane.add(btnStop, BorderLayout.CENTER);
        bottomPane.add(btnForward, BorderLayout.CENTER);


        bottomPane.add(volumeSlider, BorderLayout.EAST);
        bottomPane.add(volumeLabel, BorderLayout.EAST);
        bottomPane.add(labelTime, BorderLayout.EAST);

        panel.add(bottomPane, BorderLayout.SOUTH);

        containerPane.add(panel, BorderLayout.SOUTH);

//        playerEvents.c
//        videoFrame.addMouseListener(new MouseListener() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                System.out.println("ETS");
//            }
//
//            @Override
//            public void mousePressed(MouseEvent e) {
//                System.out.println("ETS");
//            }
//
//            @Override
//            public void mouseReleased(MouseEvent e) {
//                System.out.println("ETS");
//            }
//
//            @Override
//            public void mouseEntered(MouseEvent e) {
//                System.out.println("ETS");
//            }
//
//            @Override
//            public void mouseExited(MouseEvent e) {
//                System.out.println("ETS");
//            }
//        });
//
//        //键盘事件
//        containerPane.addKeyListener(new KeyAdapter() {
//            @Override
//            public void keyTyped(KeyEvent e) {
//                System.out.println("keyTyped");
//            }
//
//            @Override
//            public void keyPressed(KeyEvent e) {
//                System.out.println(e.getKeyCode());
//            }
//
//            @Override
//            public void keyReleased(KeyEvent e) {
//
//            }
//        });
//        containerPane.setFocusable(true);
    }

    //初始化 界面
    private void initFrame() {
        videoFrame = new JFrame("一起看");
        videoFrame.setSize(1000, 600);
        videoFrame.setLocationRelativeTo(null);
        videoFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //界面容器
        containerPane = videoFrame.getContentPane();
        containerPane.setLayout(new BorderLayout(0, 0));

        //实例化视频容器
        videoPane = new JPanel();
        videoPane.setLayout(new BorderLayout(0, 0));
        containerPane.add(videoPane, BorderLayout.CENTER);
    }

    //初始化 播放器
    private void initPlayer() {
        //创建一个播放器对象
        player = new EmbeddedMediaPlayerComponent();
        playerControls = player.mediaPlayer().controls();
        playerEvents = player.mediaPlayer().events();
        playerMedia = player.mediaPlayer().media();
        playerStatus = player.mediaPlayer().status();
        playerVideo = player.mediaPlayer().video();
        playerAudio = player.mediaPlayer().audio();

        videoPane.add(player, BorderLayout.CENTER);
        videoPane.setVisible(true);
    }

    //初始化 播放器事件
    private void initPlayerEvents() {
        player.mediaPlayer().events().addMediaPlayerEventListener(new MediaPlayerEventListener() {
            @Override
            public void mediaChanged(MediaPlayer mediaPlayer, MediaRef mediaRef) {
                System.out.println("mediaChanged");
            }

            @Override
            public void opening(MediaPlayer mediaPlayer) {
                System.out.println("opening");
            }

            @Override
            public void buffering(MediaPlayer mediaPlayer, float v) {
//                System.out.println("buffering");
            }

            @Override
            public void playing(MediaPlayer mediaPlayer) {
                System.out.println("playing");
                websock.putEvent(websock.EVENT_PLAY);
            }

            @Override
            public void paused(MediaPlayer mediaPlayer) {
                System.out.println("paused");
                websock.putEvent(websock.EVENT_PAUSE);
            }

            @Override
            public void stopped(MediaPlayer mediaPlayer) {
                System.out.println("stopped");
                websock.isRecvOneTime = true;
//                websock.putEvent(websock.EVENT_STOP);
            }

            @Override
            public void forward(MediaPlayer mediaPlayer) {
                System.out.println("stopped");
            }

            @Override
            public void backward(MediaPlayer mediaPlayer) {
                System.out.println("backward");
            }

            @Override
            public void finished(MediaPlayer mediaPlayer) {
                System.out.println("finished");
            }

            @Override
            public void timeChanged(MediaPlayer mediaPlayer, long l) {
                progress.setValue((int) l);
//                System.out.println("timeChanged");
            }

            @Override
            public void positionChanged(MediaPlayer mediaPlayer, float v) {
//                System.out.println("positionChanged"+v);
            }

            @Override
            public void seekableChanged(MediaPlayer mediaPlayer, int i) {
                System.out.println("");
            }

            @Override
            public void pausableChanged(MediaPlayer mediaPlayer, int i) {
                System.out.println("pausableChanged");
            }

            @Override
            public void titleChanged(MediaPlayer mediaPlayer, int i) {
                System.out.println("titleChanged");
            }

            @Override
            public void snapshotTaken(MediaPlayer mediaPlayer, String s) {
                System.out.println("snapshotTaken");
            }

            @Override
            public void lengthChanged(MediaPlayer mediaPlayer, long l) {
                System.out.println("lengthChanged");
            }

            @Override
            public void videoOutput(MediaPlayer mediaPlayer, int i) {
                System.out.println("videoOutput");
            }

            @Override
            public void scrambledChanged(MediaPlayer mediaPlayer, int i) {
                System.out.println("scrambledChanged");
            }

            @Override
            public void elementaryStreamAdded(MediaPlayer mediaPlayer, TrackType trackType, int i) {
                System.out.println("elementaryStreamAdded");
            }

            @Override
            public void elementaryStreamDeleted(MediaPlayer mediaPlayer, TrackType trackType, int i) {
                System.out.println("elementaryStreamDeleted");
            }

            @Override
            public void elementaryStreamSelected(MediaPlayer mediaPlayer, TrackType trackType, int i) {
                System.out.println("elementaryStreamSelected");
            }

            @Override
            public void corked(MediaPlayer mediaPlayer, boolean b) {
                System.out.println("corked");
            }

            @Override
            public void muted(MediaPlayer mediaPlayer, boolean b) {
                System.out.println("muted");
            }

            @Override
            public void volumeChanged(MediaPlayer mediaPlayer, float v) {
                volumeLabel.setText(String.format("%%%d", (int) (v * 100)));
                System.out.println(String.format("volumeChanged %f", v));
            }

            @Override
            public void audioDeviceChanged(MediaPlayer mediaPlayer, String s) {
                System.out.println("audioDeviceChanged");
            }

            @Override
            public void chapterChanged(MediaPlayer mediaPlayer, int i) {
                System.out.println("chapterChanged");
            }

            @Override
            public void error(MediaPlayer mediaPlayer) {
                System.out.println("error");
            }

            @Override
            public void mediaPlayerReady(MediaPlayer mediaPlayer) {
                long duration = playerMedia.info().duration();

                //显示总时长
                long hour = duration / (1000 * 60 * 60);
                long second = duration / 1000 % 60;
                long minute = duration / (1000 * 60) % 60;
                labelTime.setText(String.format("%d:%d:%d", hour, minute, second));

                if (duration > Integer.MAX_VALUE) {
                    System.out.println("文件太大");
                    JOptionPane.showMessageDialog(null, "文件太大", "错误", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                progress.setMaximum((int) duration);
                System.out.println("mediaPlayerReady");
            }
        });
    }

    //初始化
    public void init() {
        initFrame();
        initPlayer();
        initPlayerEvents();
        initFrameControl();

//        setVisible(true);
        setVisible(false);
    }

    //开始播放
    public void start() {
        class Start {
            public void error() {
                JOptionPane.showMessageDialog(null, "非法操作", "错误", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }

            public void quit() {
                JOptionPane.showMessageDialog(null, "用户已取消操作", "用户取消", JOptionPane.QUESTION_MESSAGE);
                System.exit(0);
            }

            //加入房间
            public boolean joinRoom() {

                try {
                    OkHttpClient client = new OkHttpClient();

                    MediaType JSON = MediaType.parse("application/json; charset=utf-8");

                    //body
                    JSONObject json = new JSONObject();
                    json.put("id", roomId);

                    RequestBody body = RequestBody.create(JSON, json.toJSONString());

                    Request req = new Request.Builder()
                            .url(Config.ServerUrl_getRoom)
                            .post(body)
                            .build();
                    //同步请求
                    Call call = client.newCall(req);
                    Response response = call.execute();

                    //解析返回内容
                    String res = response.body().string();
                    JSONObject resJson = JSONObject.parseObject(res);
                    if (resJson.getBoolean("success")) {
                        url = resJson.getString("url");
                        roomName = resJson.getString("room");
                    } else {
                        throw new Exception(resJson.getString("msg"));
                    }

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    return false;
                }

                return true;
            }

            //创建房间
            public boolean createRoom() {

                try {
                    OkHttpClient client = new OkHttpClient();

                    MediaType JSON = MediaType.parse("application/json; charset=utf-8");

                    //body
                    JSONObject json = new JSONObject();
                    json.put("url", url);
                    json.put("room", roomName);

                    RequestBody body = RequestBody.create(JSON, json.toJSONString());

                    Request req = new Request.Builder()
                            .url(Config.ServerUrl_createRoom)
                            .post(body)
                            .build();
                    //同步请求
                    Call call = client.newCall(req);
                    Response response = call.execute();

                    //解析返回内容
                    String res = response.body().string();
                    JSONObject resJson = JSONObject.parseObject(res);
                    if (resJson.getBoolean("success")) {
                        roomId = resJson.getString("id");
                    } else {
                        throw new Exception(resJson.getString("msg"));
                    }

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    return false;
                }

                return true;
            }

            public void start() {
                int isCreate = JOptionPane.showConfirmDialog(null, "是否需要创建房间？", "开始", JOptionPane.YES_NO_OPTION);
                if (isCreate == JOptionPane.CLOSED_OPTION) {
                    quit();
                }
                if (isCreate == JOptionPane.NO_OPTION) {
                    inputId();
                } else if (isCreate == JOptionPane.YES_OPTION) {
                    inputUrl();
                } else {
                    error();
                }
            }

            public void inputId() {
                roomId = JOptionPane.showInputDialog(null, "请输入房间id", "加入房间", JOptionPane.INFORMATION_MESSAGE);
                if (roomId == null) {
                    start();
                    return;
                }

                //加入房间
                if (!joinRoom()) {
                    JOptionPane.showMessageDialog(null, "加入房间失败", "错误", JOptionPane.ERROR_MESSAGE);
                    start();
                    return;
                }

                JOptionPane.showMessageDialog(null, "即将加入房间“" + roomName + "”", "确认房间信息", JOptionPane.INFORMATION_MESSAGE);
            }

            public void inputUrl() {
                url = JOptionPane.showInputDialog(null, "请输入视频链接", "创建房间", JOptionPane.INFORMATION_MESSAGE);
                if (url == null) {
                    start();
                    return;
                }
                roomName = JOptionPane.showInputDialog(null, "请输入房间名", "创建房间", JOptionPane.INFORMATION_MESSAGE);
                if (roomName == null) {
                    start();
                    return;
                }

                //创建房间
                if (!createRoom()) {
                    JOptionPane.showMessageDialog(null, "创建房间失败", "错误", JOptionPane.ERROR_MESSAGE);
                    start();
                    return;
                }

                JOptionPane.showInputDialog(null, "为您分配的房间id为", roomId);
            }
        }

        //开启前内容输入
        Start s = new Start();
        s.start();

        //设置标题
        videoFrame.setTitle(roomName);
        //显示界面
        setVisible(true);
        websock = new Websock(Config.webSocktUrl_relayMessage + roomId);
        //播放视频
        playerMedia.play(url);

        //定时上报视频进度
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (websock.isClosed())
                        websock.reconnect();
                    long time = playerStatus.time();
                    websock.putTime(false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 500);
    }

    public void setVisible(boolean b) {
        videoFrame.setVisible(b);
    }

}
