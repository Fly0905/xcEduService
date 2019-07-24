package com.xuecheng.auth;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;


/**
 * 测试使用BCrypt加密
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class TestBCrypt {

    @Test
    public void testPasswordEncoder() {
        String password = "111111";
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        for (int i = 0; i < 10; i++) {
            //每个计算出的Hash值都不一样
            String hashPass = passwordEncoder.encode(password);
            System.out.println(hashPass);
            //虽然每次计算的密码Hash值不一样但是校验是通过的
            boolean f = passwordEncoder.matches(password, hashPass);
            System.out.println(f);
        }
    }
}
