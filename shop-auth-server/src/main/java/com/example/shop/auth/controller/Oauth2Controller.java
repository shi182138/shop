package com.example.shop.auth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.example.common.constant.AuthServerConstant;
import com.example.common.to.SocialUserTo;
import com.example.common.utils.HttpUtils;
import com.example.common.utils.R;
import com.example.common.vo.MemberRespVo;
import com.example.shop.auth.feign.MemberFeignService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;

import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Controller
public class Oauth2Controller {
    @Autowired
    private MemberFeignService memberFeignService;

    //用户同意授权 使用 code 获取 token，使用 token 获取 用户数据
// http://auth.gulimao.com/oauth2/gitee/success
    @RequestMapping("/oauth2/weibo/success")
    public String authorize(@RequestParam("code") String code, HttpSession session) throws Exception {
        //1.使用code换取token，换取成功继续2，否则重定向至登录页
        Map<String,String> query = new HashMap<>();
        query.put("client_id","af67f8d864876854f991a2c73c13c4332630250b42fd5c27ce16278d183cea88");
        query.put("client_secret","37284a60454504dd875074f453e10c53574a7bee0ed52d84278f2696285143dc");
        query.put("grant_type","authorization_code");
        query.put("redirect_uri","http://auth.gulimao.com/oauth2/weibo/success");
        query.put("code",code);

        //2.发送 post 请求获取 token
        //https://gitee.com/oauth/token?grant_type=authorization_code&code={code}&client_id={client_id}&redirect_uri={redirect_uri}&client_secret={client_secret}
        HttpResponse response = HttpUtils.doPost("https://gitee.com", "/oauth/token", "post", new HashMap<String, String>(), query, new HashMap<String, String>());
        Map<String,String> errors = new HashMap<>();
        if (response.getStatusLine().getStatusCode() == 200) {
            //3.调用 member 远程接口进行 oauth 登录，登录成功则转发至首页并携带返回用户信息，否则转发至登录页
            String json = EntityUtils.toString(response.getEntity());
            SocialUserTo socialUserTo = JSON.parseObject(json, SocialUserTo.class);

            //知道了哪个社交用户
            //1）、当前用户如果是第一次进网站，自动注册进来（为当前社交用户生成一个会员信息，以后这个社交账号就对应指定的会员）
            //登录或者注册这个社交用户
            System.out.println(socialUserTo.getAccess_token());
            //调用远程服务

            R oauthLogin = memberFeignService.oauth2Login(socialUserTo);

            if (oauthLogin.getCode() == 0) {
                MemberRespVo data = oauthLogin.getData("data", new TypeReference<MemberRespVo>() {
                });
                log.info("登录成功：用户信息：{}", data.toString());

                //1、第一次使用session，命令浏览器保存卡号，JSESSIONID这个cookie
                //以后浏览器访问哪个网站就会带上这个网站的cookie
                //TODO 1、默认发的令牌。当前域（解决子域session共享问题）
                //TODO 2、使用JSON的序列化方式来序列化对象到Redis中
                session.setAttribute(AuthServerConstant.LOGIN_USER, data);

                //2、登录成功跳回首页
                return "redirect:http://gulimao.com";
            } else {

                return "redirect:http://auth.gulimao.com/login.html";
            }


        } else {
            return "redirect:http://auth.gulimao.com/login.html";
        }


    }
}
