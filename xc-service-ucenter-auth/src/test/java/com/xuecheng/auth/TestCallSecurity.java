package com.xuecheng.auth;


import com.xuecheng.framework.client.XcServiceList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Base64Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

@SpringBootTest
@RunWith(SpringRunner.class)
public class TestCallSecurity {

    @Autowired
    LoadBalancerClient loadBalancerClient;

    @Autowired
    RestTemplate restTemplate;

    //远程请求Spring Security远程请求令牌
    @Test
    public void testClient(){
        //这里使用LoadBalance，可以直接从Eureka中获取对应的服务(因为Spring Security在认证服务中)
        //从Eureka中获取认证服务的一个实例的地址
        ServiceInstance serviceInstance = loadBalancerClient.choose(XcServiceList.XC_SERVICE_UCENTER_AUTH);
        //获取uri 即: http://ip:port
        URI uri = serviceInstance.getUri();
        //得到令牌的地址: http://localhost:40400/auth/oauth/token
        String authUrl = uri + "/auth/oauth/token";

        //下面要开始请求了，但是需要带上Header和Body，所以需要先定义Header和Body

        // (1)、定义Header
        LinkedMultiValueMap<String, String> header = new LinkedMultiValueMap<>();
        String httpBasic = getHttpBasic("XcWebApp", "XcWebApp");
        header.add("Authorization", httpBasic);
        // (2)、定义Body
        LinkedMultiValueMap<String, String>body = new LinkedMultiValueMap<>();
        body.add("grant_type","password");
        //暂时写死在  认证服务的UserDetailsServiceImpl中的，后面要写到数据库的
        body.add("username","itcast");
        body.add("password","123");


        //组装一个Http实例，将body和header放到其中
        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(body, header);


        //因为如果密码/用户名错误，就会抛出异常，终止返回，返回结果得不到信息(错误信息)，所以这里设置(400/401也要返回信息(错误信息))
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler(){
            //让400/401不报错，也要返回数据
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                if(response.getRawStatusCode()!=400 && response.getRawStatusCode()!=401){
                    super.handleError(response);
                }
            }
        });

        //可以开始调用了
        //调用  参数: String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity, Class<T> responseType, Object... uriVariables
        ResponseEntity<Map> exchange = restTemplate.exchange(authUrl, HttpMethod.POST, httpEntity, Map.class);

        //获取申请令牌信息
        Map jwtMsg = exchange.getBody();
        System.out.println(jwtMsg);
    }

    //即获取到  由格式  "用户名:密码"  的base64编码  （HttpBasic认证的方法）
    private String getHttpBasic(String clientId, String clientSecret) {
        String str = clientId + ":" + clientSecret;
        byte[] encode = Base64Utils.encode(str.getBytes());
        return "Basic " + new String(encode) ; // 提交请求的格式   "Basic 编码"    (注意有一个空格)
    }
}
