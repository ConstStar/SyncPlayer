package com.example.Server;

import com.alibaba.fastjson.*;
import com.alibaba.fastjson.util.IOUtils;

import java.io.*;
import java.util.Map;
import javax.servlet.http.*;
import javax.servlet.annotation.*;

//获取评论
@WebServlet(name = "CreateHome", value = "/create_home")
public class CreateHome extends HttpServlet {
    public void init() {
    }

    static boolean b = false;

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain;charset=utf-8");

        PrintWriter out = response.getWriter();
        //返回数据
        JSONObject object = new JSONObject();

        try {
            String body = IOUtils.readAll(request.getReader());
            JSONObject bodyObject = JSONObject.parseObject(body);

            //获取参数
            String url = bodyObject.getString("url");
            String home = bodyObject.getString("home");

            String id;
            //随机房间id
            while (true) {
                id = String.valueOf(System.currentTimeMillis() % 1000000000);
                Server.Inf inf = Server.inf.get(id);
                if (inf == null) {
                    Server.inf.put(id, new Server.Inf(url,home));
                    break;
                }
                Thread.sleep(3);
            }

            object.put("id", id);

            object.put("success", true);
            object.put("msg", "房间创建成功");
        } catch (Exception ex) {
            object.put("success", false);
            object.put("msg", ex.getMessage());
        }

        out.print(object.toString());
    }

    public void destroy() {
    }

}
