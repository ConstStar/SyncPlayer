import uk.co.caprica.vlcj.player.base.ControlsApi;
import uk.co.caprica.vlcj.player.base.EventApi;
import uk.co.caprica.vlcj.player.base.MediaApi;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;

import javax.swing.*;
import java.awt.*;

public class Test {
    JFrame videoFrame; //JFrame 实例
    Container containerPane;//界面容器

    EmbeddedMediaPlayerComponent player; //视频播放器
    ControlsApi playerControls; //视频控制器
    EventApi playerEvents; //视频控制器
    MediaApi playerMedia;  //视频媒体

    JPanel videoPane;//视频容器
    JPanel panel;   //控制区域容器
    JPanel progressPanel;   //进度条容器
    JProgressBar progress;  //进度条

    public EmbeddedMediaPlayerComponent getPlayer() {
        return player;
    }

    //初始化 界面
    private void initFrame(){
        videoFrame = new JFrame("Test");
        videoFrame.setSize(1000, 600);
        videoFrame.setLocationRelativeTo(null);
        videoFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //界面容器
        containerPane = videoFrame.getContentPane();
        containerPane.setLayout(new BorderLayout());

        //实例化视频容器
        videoPane=new JPanel();
        videoPane.setLayout(new BorderLayout());
        containerPane.add(videoPane,BorderLayout.CENTER);
    }

    private void initPlayer(){
        player=new EmbeddedMediaPlayerComponent();
        videoPane.add(player,BorderLayout.CENTER);
    }

    public void init() {
        initFrame();



        JButton btnPlay=new JButton("播放");
        JButton btnStop=new JButton("停止");
        JButton btnPause=new JButton("暂停");

        JPanel bottomPane=new JPanel();
        bottomPane.add(btnPlay);
        bottomPane.add(btnStop);
        bottomPane.add(btnPause);

        containerPane.add(bottomPane,BorderLayout.SOUTH);


        videoFrame.setVisible(true);
    }
}
