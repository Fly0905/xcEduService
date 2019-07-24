package com.xuecheng.api.filesystem;

import com.xuecheng.framework.domain.filesystem.response.UploadFileResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.multipart.MultipartFile;


@Api(value = "文件管理接口", tags = "文件管理接口，提供文件的上传、下载、查询等")
public interface FileSystemControllerApi {

    /**
     * 上传文件
     * @param multipartFile 文件
     * @param filetag       文件标签
     * @param businesskey   业务key
     * @param metedata      元信息,json格式
     * @return
     */
    @ApiOperation("上传文件接口")
    UploadFileResult upload(MultipartFile multipartFile,
                            String filetag,
                            String businesskey,
                            String metadata);
}
