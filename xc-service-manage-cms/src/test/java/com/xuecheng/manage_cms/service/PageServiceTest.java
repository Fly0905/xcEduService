package com.xuecheng.manage_cms.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class PageServiceTest {

    @Autowired
    PageService pageService;

    @Test
    public void testGetPageHtml(){

        // 5cdac28e4dee6c23e82875ed
//        String pageHtml = pageService.getPageHtml("5bf02330e6562553c8888950");

        String pageHtml = pageService.getPageHtml("5cdac28e4dee6c23e82875ed");

        System.out.println(pageHtml);
    }
}