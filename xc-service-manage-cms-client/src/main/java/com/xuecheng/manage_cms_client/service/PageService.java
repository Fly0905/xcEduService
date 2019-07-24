package com.xuecheng.manage_cms_client.service;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.CmsSite;
import com.xuecheng.manage_cms_client.dao.CmsPageRepository;
import com.xuecheng.manage_cms_client.dao.CmsSiteRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Service
@Slf4j
public class PageService {

    @Autowired
    CmsPageRepository cmsPageRepository;

    @Autowired
    CmsSiteRepository cmsSiteRepository;

    // 从GridFs中取内容
    @Autowired
    GridFsTemplate gridFsTemplate;

    @Autowired
    GridFSBucket gridFSBucket;

    //接受MQ消息之后要调用这个方法，然后把文件从GridFS中下载下来，然后保存到服务器
    public void savePageToServerPath(String pageId) {
        // 根据pageId查询CmsPage
        CmsPage cmsPage = this.findCmsPageById(pageId);

        //得到html的文件id，从cmsPage中获取htmlFileId内容
        String htmlFileId = cmsPage.getHtmlFileId();

        // 从GridFs中查询html文件
        InputStream inputStream = this.getFileById(htmlFileId);
        if (inputStream == null) {
            log.error("getFileById InputStream is null, htmlFileId:{}", htmlFileId);
            return;
        }
        // 得到站点Id
        String siteId = cmsPage.getSiteId();
        // 得到站点的信息　
        CmsSite cmsSite = this.findCmsSiteById(siteId);
        // 得到站点的物理路径
        String sitePhysicalPath = cmsSite.getSitePhysicalPath();

        //页面物理路径
        String pagePath = sitePhysicalPath + cmsPage.getPagePhysicalPath() + cmsPage.getPageName();

        // 将 html文件保存在服务器物理路径上
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(new File(pagePath)); // 保存到服务器对应位置
            IOUtils.copy(inputStream, fileOutputStream);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public CmsPage findCmsPageById(String pageId) {
        Optional<CmsPage> optional = cmsPageRepository.findById(pageId);
        if (optional.isPresent()) {
            return optional.get();
        }
        return null;
    }

    //根据htmlFileId从GridFs中查询文件的内容
    public InputStream getFileById(String fileId) {
        // 文件对象
        GridFSFile gridFSFile = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(fileId)));
        // 打开一个下载流
        GridFSDownloadStream gridFSDownloadStream = gridFSBucket.openDownloadStream(gridFSFile.getObjectId());
        // 定义一个GridFsResources
        GridFsResource gridFsResource = new GridFsResource(gridFSFile, gridFSDownloadStream);
        try {
            return gridFsResource.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public CmsSite findCmsSiteById(String siteId) {
        Optional<CmsSite> optional = cmsSiteRepository.findById(siteId);
        if (optional.isPresent()) {
            return optional.get();
        }
        return null;
    }
}
