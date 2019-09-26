package com.atguigu.gamll.passport.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.JwtUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    String signKey = "Atguigu_Gmall_Key";

    @Reference
    UserService userService;


    @RequestMapping("index")
    public String index(HttpServletRequest request) {
        String originUrl = request.getParameter("originUrl");
        // 保存上
        request.setAttribute("originUrl", originUrl);
        return "index";

    }


    //验证token是否正确
    @GetMapping("verify")
    @ResponseBody
    public String verify(@RequestParam("token") String token, @RequestParam("currentIP") String currentIp) {
        //1 验证token
        Map<String, Object> map = JwtUtil.decode(token, signKey, currentIp);

        if (token != null) {
            String id = (String) map.get("userId");
            UserInfo userInfo = userService.verify(id);
            //2 验证缓存
            if(userInfo!=null){
                return "success";
            }
        }

        return "fail";

    }

    //登录，创造一个Token发送给用户
    @PostMapping("login")
    @ResponseBody
    public String loginI(UserInfo userInfo, HttpServletRequest request) {
        UserInfo login = userService.login(userInfo);

        if (login != null) {
            String header = request.getHeader("X-forwarded-for");
            Map map = new HashMap<>();
            map.put("userId", login.getId());
            map.put("nickName", login.getNickName());
            String token = JwtUtil.encode(signKey, map, header);
            return token;
        }
        return "fail";
    }


}
