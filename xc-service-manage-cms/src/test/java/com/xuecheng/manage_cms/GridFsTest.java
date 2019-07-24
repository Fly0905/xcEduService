package com.xuecheng.manage_cms;


import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

@SpringBootTest
@RunWith(SpringRunner.class)
public class GridFsTest {

    @Autowired
    GridFsTemplate gridFsTemplate;

    //存文件2, 将文件存到GridFS中
    @Test
    public void testSaveFile() throws FileNotFoundException {
        //要存储的文件
//        File file = new File("D:/xcEduService01/test-freemarker/src/main/resources/templates/index_banner.ftl");
        File file = new File("D:/xcCode/xcEduService01/test-freemarker/src/main/resources/templates/index_banner.ftl");
        //定义输入流
        FileInputStream inputStream = new FileInputStream(file);
        //向GridFS存储文件
        ObjectId objectId = gridFsTemplate.store(inputStream, "轮播图测试文件01", "");
        System.out.println(objectId);
        //得到文件ID
        String fileId = objectId.toString();
//        System.out.println(file);
    }


    @Test
    public void testStore2() throws FileNotFoundException {
        String path = "D:/xcEduService01/test-freemarker/src/main/resources/templates/course.ftl";
        File file = new File(path);
        FileInputStream inputStream = new FileInputStream(file);
        //保存模版文件内容
        ObjectId objectId = gridFsTemplate.store(inputStream, "课程详情模板文件","");
        System.out.println(objectId);
    }

    @Autowired
    GridFSBucket gridFSBucket;

    @Test
    public void queryFile() throws IOException {
        String fileId = "5d1dbe4edc7ca412740e16ce";
        //根据id查询文件
        GridFSFile gridFSFile = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(fileId)));
        //打开下载流对象
        GridFSDownloadStream gridFSDownloadStream = gridFSBucket.openDownloadStream(gridFSFile.getObjectId());
        //创建gridFsResource，用于获取流对象
        GridFsResource gridFsResource = new GridFsResource(gridFSFile, gridFSDownloadStream);
        //获取流中的数据
        String s = IOUtils.toString(gridFsResource.getInputStream(), "UTF-8");
        System.out.println(s);
    }


    //删除文件
    @Test
    public void testDelFile() throws IOException {
        //根据文件id删除fs.files和fs.chunks中的记录
        gridFsTemplate.delete(Query.query(Criteria.where("_id").is("5b32480ed3a022164c4d2f92")));
    }
}
