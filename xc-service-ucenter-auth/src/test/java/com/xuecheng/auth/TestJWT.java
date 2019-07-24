package com.xuecheng.auth;

import com.alibaba.fastjson.JSON;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaSigner;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;
import org.springframework.test.context.junit4.SpringRunner;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
@RunWith(SpringRunner.class)
public class TestJWT {

    @Test
    public void testJWT() {
        //证书文件
        String key_location = "xc.keystore"; // 类路径下，就是刚刚生成的私钥的文件
        //密钥库密码
        String keystore_password = "xuechengkeystore";
        //访问证书路径
        ClassPathResource resource = new ClassPathResource(key_location);
        //密钥工厂
        KeyStoreKeyFactory keyStoreKeyFactory = new KeyStoreKeyFactory(resource,
                keystore_password.toCharArray());

        //密钥的密码，此密码和别名要匹配
        String keyPassword = "xuecheng";
        //密钥别名
        String alias = "xckey";
        //密钥对（密钥和公钥）
        KeyPair keyPair = keyStoreKeyFactory.getKeyPair(alias, keyPassword.toCharArray());
        //私钥
        RSAPrivateKey aPrivate = (RSAPrivateKey) keyPair.getPrivate();


        //定义payload信息 (jwt第二部分的内容)
        Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put("id", "123");
        tokenMap.put("name", "mrt");
        tokenMap.put("roles", "r01,r02");
        tokenMap.put("ext", "1");


        //可以生成JWT令牌了
        Jwt jwt = JwtHelper.encode(JSON.toJSONString(tokenMap), new RsaSigner(aPrivate));

        //取出jwt令牌
        String token = jwt.getEncoded();
        System.out.println("token = " + token);

        /**
         * 最后生成的jwt令牌如下:
         * eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHQiOiIxIiwicm9sZXMiOiJyMDEscjAyIiwibmFtZSI6Im1ydCIsImlkIjoiMTIzIn0.ajVHKM2Dts5zVpBUoIIStPQ1aWTVlEeF1hw7DcGKINt4Wj6wgfkxTRHbt7s4x42-w0BCfNMpf4b5wjXEhH9NTPT-BxsxznJQxJiEnqAn8RtbdWTDel1LjTlgm9PQ4uiLD-ksoKqgN8gsypcJAuw_nokI463cif8ueEAW5SqujSNMJExI9MkZBBf4TVAX1Fjq42UUFWpdGbM-WnPi3bwDqE2palKrmEUtHI1AixBWCuDyfT-Zxk6TISoDsY6qGfzR7O3GUfnq2SIfE63UxDMAREAloBgveHSH4Wjtik7vLMOUs9n-Y6yXmcSUIt2FdZWTLs03IRi1pRzb7L8DBul5zw
         */
    }

    //使用公钥校验JWT令牌
    @Test
    public void testVerifyJWT() {
        //公钥 (使用openSSL生成的， 在xc-service-manage-course下的publickey.txt下有，从那里拷贝即可)
        String publickey = "-----BEGIN PUBLIC KEY-----MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjnyoVu8WCrDrgbfnD2lpB3FnBhN1Kb9ZqKGR8J4Y2vAR4gM7iOgOO94eTE0/sMXy3usp/mfh+7Oco2/5d6GUQbIp0oMm/XjUVaeKcinT3XVfgz97c9RTTPcefHY5g2kkWhtYoZFG6IDPkxjJ7BFcMGG5ZKfBK9z21jjdhGPjXCCNNatyhr8INKPsQMJbupKKMObGgR66nm1cF13CQP3OCgMo2tHPMINv/txp5natJI7Kb53b6t7ycyP5/KFSYKIxofA5kX9tpSeEpoGjFu/oVm68Tg1pnC+Du2ziYkk2iG+Ae5nryRAjbmCWL56wBDgKkWJ9zddKt/URMRBOVomr6QIDAQAB-----END PUBLIC KEY-----";
        //校验jwt  (如果出错，就是校验失败)

        //jwt令牌
//        String jwtString = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb21wYW55SWQiOiIxIiwidXNlcnBpYyI6bnVsbCwidXNlcl9uYW1lIjoiaXRjYXN0Iiwic2NvcGUiOlsiYXBwIl0sIm5hbWUiOiJ0ZXN0MDIiLCJ1dHlwZSI6IjEwMTAwMiIsImlkIjoiNDkiLCJleHAiOjE1NjEwNzc3NTAsImF1dGhvcml0aWVzIjpbInhjX3RlYWNobWFuYWdlcl9jb3Vyc2VfYmFzZSIsInhjX3RlYWNobWFuYWdlcl9jb3Vyc2VfZGVsIiwieGNfdGVhY2htYW5hZ2VyX2NvdXJzZV9saXN0IiwieGNfdGVhY2htYW5hZ2VyX2NvdXJzZV9wbGFuIiwieGNfdGVhY2htYW5hZ2VyX2NvdXJzZSIsImNvdXJzZV9maW5kX2xpc3QiLCJ4Y190ZWFjaG1hbmFnZXIiLCJ4Y190ZWFjaG1hbmFnZXJfY291cnNlX21hcmtldCIsInhjX3RlYWNobWFuYWdlcl9jb3Vyc2VfcHVibGlzaCIsInhjX3RlYWNobWFuYWdlcl9jb3Vyc2VfYWRkIl0sImp0aSI6ImYzNGI3NDE2LTEyZmEtNGFiMy1hODk3LWVkZjVmMjFlYjI0YiIsImNsaWVudF9pZCI6IlhjV2ViQXBwIn0.fVIeG03TT3t8ZQ4bnY0dWK64XOpPHKa3qvkNN1WK7d72Nnv-2sx69BsMsqC-tEeXhsike4OpYQ8C01-p9Te4EpocO8r169QmThXOBkGUchUV4jycCH1tpRDTYPKUbBEjU_IBuYt8yV5GniieMxgOu1KqcbCByI-0IwdS026YGuyqHd3AhaWKACptFdVVYbqOSIjSHTQKdNqKyhFSJksZdDj3G5o6F-8vlWuJnT1RgMCK5FNpchNhbDD5bXTRTaaVpH8ZJNellY8u44Jlt8iolASONJxZJN6fIUdNyZwA0K0Ln2idxFG_CEUMAmFzenllB72jujzTvH6-7ILBjem0-g";
        String jwtString = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHQiOiIxIiwicm9sZXMiOiJyMDEscjAyIiwibmFtZSI6Im1ydCIsImlkIjoiMTIzIn0.ajVHKM2Dts5zVpBUoIIStPQ1aWTVlEeF1hw7DcGKINt4Wj6wgfkxTRHbt7s4x42-w0BCfNMpf4b5wjXEhH9NTPT-BxsxznJQxJiEnqAn8RtbdWTDel1LjTlgm9PQ4uiLD-ksoKqgN8gsypcJAuw_nokI463cif8ueEAW5SqujSNMJExI9MkZBBf4TVAX1Fjq42UUFWpdGbM-WnPi3bwDqE2palKrmEUtHI1AixBWCuDyfT-Zxk6TISoDsY6qGfzR7O3GUfnq2SIfE63UxDMAREAloBgveHSH4Wjtik7vLMOUs9n-Y6yXmcSUIt2FdZWTLs03IRi1pRzb7L8DBul5zw";

        Jwt jwt = JwtHelper.decodeAndVerify(jwtString, new RsaVerifier(publickey));

        //拿到当初jwt中自定义的内容(第二部分)

        //获取jwt原始内容
        String claims = jwt.getClaims();
        System.out.println(claims); //校验失败

        //jwt令牌
        String encoded = jwt.getEncoded();
        System.out.println(encoded);
    }
}
