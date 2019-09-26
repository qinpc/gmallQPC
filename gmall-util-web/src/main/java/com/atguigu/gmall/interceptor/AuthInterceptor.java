package com.atguigu.gmall.interceptor;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.constants.WebConst;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpClientUtil;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        //token可能存在 1 url参数  newToken   2 从cookie中获得 token
        String token = "";
        token = request.getParameter("newToken");
        if (token != null) {
            //把token保存到cookie中
            CookieUtil.setCookie(request, response, "token", token, WebConst.COOKIE_MAXAGE, false);
        } else {
            //从cookie中取值  token
            token = CookieUtil.getCookieValue(request, "token", false);
        }

        //从token中把用户信息取出来
        Map userMap = new HashMap();
        if (token != null) {
            userMap = getUserMapfromToken(token);
            String nickName = (String) userMap.get("nickName");
            request.setAttribute("nickName", nickName);
        }

        //判断是否该请求需要用户登录
        //取到请求的方法上的注解  LoginRequire
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        LoginRequire loginRequire = handlerMethod.getMethodAnnotation(LoginRequire.class);

        if (loginRequire != null) {
            //需要认证
            if (token != null) {
                String currentIP = request.getHeader("X-forwarded-for");
                //把token送去认证
                String result = HttpClientUtil.doGet(WebConst.VERIFY_ADDRESS + "?token=" + token + "&currentIP=" + currentIP);
                if ("success".equals(result)) {
                    //验证成功
                    String userId = (String) userMap.get("userId");
                    request.setAttribute("userId", userId);
                    return true;
                } else if (!loginRequire.autoRedirect()) {
                    //认证失败但是运行不跳转
                    return true;
                } else {
                    //认证失败 强行跳转
                    redirect(request, response);
                    return false;
                }
            } else { // 强行跳转
                //  进行重定向  passport 让用户登录
                if(!loginRequire.autoRedirect()) {  //认证失败但是 运行不跳转
                    return true;
                }else{
                    redirect(  request,   response);
                    return false;
                }
            }
        }
        //没有注释，放行
        return true;
    }


    private void redirect(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestURL = request.getRequestURL().toString();//取得用户的当前登录请求
        String encodeURL = URLEncoder.encode(requestURL, "UTF-8");
        response.sendRedirect(WebConst.LOGIN_ADDRESS + "?originUrl=" + encodeURL);
    }

    private Map getUserMapfromToken(String token) {
        String substringBetween = StringUtils.substringBetween(token, ".");
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        byte[] bytes = base64UrlCodec.decode(substringBetween);
        String userJson = new String(bytes);
        Map map = JSON.parseObject(userJson, Map.class);
        return map;
    }
}
