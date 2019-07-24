package com.xuecheng.manage_cms.dao;


import com.xuecheng.framework.domain.cms.CmsPage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.*;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
public class CmsPageRepositoryTest {

    @Autowired
    CmsPageRepository cmsPageRepository;

    @Test
    public void testFindAll(){
        List<CmsPage> all = cmsPageRepository.findAll();
        System.out.println(all);
    }

    // 分页查询
    @Test
    public void testFindPage(){
        int page = 0; // 页码从0开始
        int size = 10;// 每一页的显示数量
        Pageable pageable = PageRequest.of(page, size);
        Page<CmsPage> all = cmsPageRepository.findAll(pageable);
        System.out.println(all);
        System.out.println("eee");
    }

    // 根据自定义条件查询
    @Test
    public void testFindAllByExample() {

        //条件匹配器
//        ExampleMatcher exampleMatcher = ExampleMatcher.matching();
//        exampleMatcher = exampleMatcher.withMatcher("pageAliase",ExampleMatcher.
//                GenericPropertyMatchers.contains());
        // 上面的简写的方式
        ExampleMatcher exampleMatcher = ExampleMatcher.matching()
                .withMatcher("pageAliase",ExampleMatcher.GenericPropertyMatchers.contains());

        //页面别名模糊查询，需要自定义字符串的匹配器实现模糊查询
        //ExampleMatcher.GenericPropertyMatchers.contains() 包含
        //ExampleMatcher.GenericPropertyMatchers.startsWith()//开头匹配
        //条件值
        CmsPage cmsPage = new CmsPage();
        //站点ID
        cmsPage.setSiteId("5a751fab6abb5044e0d19ea1");

        cmsPage.setPageAliase("课程");
        //创建条件实例
        Example<CmsPage> example = Example.of(cmsPage, exampleMatcher);

        Pageable pageable = PageRequest.of(0, 10);
        Page<CmsPage> all = cmsPageRepository.findAll(example, pageable);
        System.out.println(all);
    }

}
