import com.alibaba.fastjson.JSONObject;
import okhttp3.*;

import javax.swing.*;
import java.io.IOException;
import java.util.Date;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        Video v = new Video();
        v.init();
        v.start();
    }
}