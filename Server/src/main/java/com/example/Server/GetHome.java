package com.example.Server;

import com.alibaba.fastjson.*;
import com.alibaba.fastjson.util.IOUtils;

import java.io.*;
import java.util.Map;
import javax.servlet.http.*;
import javax.servlet.annotation.*;

//获取评论
@WebServlet(name = "GetHome", value = "/get_home")
public class GetHome extends HttpServlet {
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
            String id = bodyObject.getString("id");

            //获取房间信息
            Server.Inf inf = Server.inf.get(id);
            if(inf==null){
                throw new Exception("没有找到房间信息");
            }

            object.put("url", inf.url);
            object.put("home", inf.home);
//            object.put("time", inf.time);

            object.put("success", true);
            object.put("msg", "获取房间信息成功");
        } catch (Exception ex) {
            object.put("success", false);
            object.put("msg", ex.getMessage());
        }

        out.print(object.toString());
    }

    public void destroy() {
    }

}
