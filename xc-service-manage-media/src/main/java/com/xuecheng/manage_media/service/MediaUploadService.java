package com.xuecheng.manage_media.service;


import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.response.CheckChunkResult;
import com.xuecheng.framework.domain.media.response.MediaCode;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_media.config.RabbitMQConfig;
import com.xuecheng.manage_media.dao.MediaFileRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

@Service
@Slf4j
public class MediaUploadService {

    @Autowired
    MediaFileRepository mediaFileRepository;

    @Value("${xc-service-manage-media.upload-location}")
    String uploadLocation;

    @Autowired
    RabbitTemplate rabbitTemplate;

    //视频处理路由
    @Value("${xc-service-manage-media.mq.routingkey-media-video}")
    public String routingkey_media_video;


    //一、文件上传前的注册
    public ResponseResult register(String fileMd5, String fileName, Long fileSize, String mimetype, String fileExt) {
        //1、检查文件在磁盘中是否存在
        // 文件所属的目录的路径
        String fileFolderPath = getFileFolderPath(fileMd5);
        //文件路径
        String filePath = getFilePath(fileMd5, fileExt);
        File file = new File(filePath);

        //2、检查文件信息在mongodb中是否存在
        Optional<MediaFile> optional = mediaFileRepository.findById(fileMd5);
        //文件存在直接返回
        if (file.exists() && optional.isPresent()) {
            ExceptionCast.cast(MediaCode.UPLOAD_FILE_REGISTER_EXIST);
        }
        //文件不存在时做一些准备工作，检查文件所在目录是否存在，如果不存在则创建
        File fileFolder = new File(fileFolderPath);
        if (!fileFolder.exists()) {
            fileFolder.mkdirs();
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }

    /**
     * 根据文件md5得到文件路径
     * 规则：
     * 一级目录：md5的第一个字符
     * 二级目录：md5的第二个字符
     * 三级目录：md5
     * 文件名：md5+文件扩展名
     *
     * @param fileMd5 文件md5值
     * @param fileExt 文件扩展名
     * @return 文件路径
     */
    //文件路径
    private String getFilePath(String fileMd5, String fileExt) {
        return getFileFolderPath(fileMd5) + fileMd5 + "." + fileExt;
    }

    //得到文件所在目录
    private String getFileFolderPath(String fileMd5) {
        return uploadLocation + getFileFolderRelativePath(fileMd5);
    }

    //得到文件目录相对路径，路径中去掉根目录
    private String getFileFolderRelativePath(String fileMd5) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/";
    }

    //二、检查分块文件是否存在
    public CheckChunkResult checkchunk(String fileMd5, Integer chunk, Integer chunkSize) {
        //得到块文件所在路径
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        //块文件的文件名称以1,2,3..序号命名，没有扩展名
        File chunkFile = new File(chunkFileFolderPath + chunk);

        if (chunkFile.exists()) {
            return new CheckChunkResult(MediaCode.CHUNK_FILE_EXIST_CHECK, true);
        } else {
            return new CheckChunkResult(MediaCode.CHUNK_FILE_EXIST_CHECK, false);
        }
    }

    //得到块文件所在目录
    private String getChunkFileFolderPath(String fileMd5) {
        return getFileFolderPath(fileMd5) + "/" + "chunk" + "/";
    }

    //三、上传分块
    public ResponseResult uploadchunk(MultipartFile file, Integer chunk, String fileMd5) {
        if (file == null) {
            ExceptionCast.cast(MediaCode.UPLOAD_FILE_REGISTER_ISNULL);
        }
        // 如果块文件目录不存在需要创建
        File chunkFolderFile = new File(getChunkFileFolderPath(fileMd5));
        if (!chunkFolderFile.exists()) {
            chunkFolderFile.mkdirs();
        }

        //块文件路径
        File chunkFile = new File(getChunkFileFolderPath(fileMd5) + chunk);

        //上传块文件
        InputStream is = null;
        FileOutputStream os = null;
        try {
            is = file.getInputStream();
            os = new FileOutputStream(chunkFile);
            IOUtils.copy(is, os);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("upload chunk file fail:{}", e.getMessage());
            ExceptionCast.cast(MediaCode.CHUNK_FILE_UPLOAD_FAIL);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }

    // 四、合并分块
    public ResponseResult mergechunks(String fileMd5, String fileName, Long fileSize, String mimetype, String fileExt) {
        // 1、合并所有分块
        File chunkFolderPathFile = new File(getChunkFileFolderPath(fileMd5));
        if (!chunkFolderPathFile.exists()) {
            chunkFolderPathFile.mkdirs();
        }

        //合并文件路径
        File mergeFile = new File(getFilePath(fileMd5, fileExt));
        //如果合并文件已存在则删除，否则创建新文件
        try {
            if (mergeFile.exists()) {
                mergeFile.delete();
            } else {
                //创建一个新文件
                mergeFile.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //文件列表
        File[] files = chunkFolderPathFile.listFiles();
        List<File> chunkFileList = Arrays.asList(files);
        //对块文件进行排序
        Collections.sort(chunkFileList, (o1, o2) -> {
            if (Integer.parseInt(o1.getName()) > Integer.parseInt(o2.getName())) {
                return 1;
            }
            return -1;
        });
        //合并工作
        mergeFile = mergeFile(mergeFile, chunkFileList);
        if (mergeFile == null) {
            ExceptionCast.cast(MediaCode.MERGE_FILE_FAIL);
        }

        // 2、校验文件
        boolean checkResult = this.checkFileMd5(mergeFile, fileMd5);
        if (!checkResult) {
            ExceptionCast.cast(MediaCode.MERGE_FILE_CHECKFAIL);
        }

        // 3、写入信息到MongoDB数据库
        MediaFile mediaFile = new MediaFile();
        mediaFile.setFileId(fileMd5);
        mediaFile.setFileName(fileMd5 + "." + fileExt);
        mediaFile.setFileOriginalName(fileName);
        //文件路径保存相对路径
        mediaFile.setFilePath(getFileFolderRelativePath(fileMd5)); //注意这里的Path是相对路径
        mediaFile.setFileSize(fileSize);
        mediaFile.setUploadTime(new Date());
        mediaFile.setMimeType(mimetype);
        mediaFile.setFileType(fileExt);
        //状态为上传成功
        mediaFile.setFileStatus("301002");
        mediaFileRepository.save(mediaFile);

        //新增代码，向MQ发送视频处理消息
        sendProcessVideoMsg(mediaFile.getFileId());

        return new ResponseResult(CommonCode.SUCCESS);
    }

    private File mergeFile(File mergeFile, List<File> chunkFileList) {
        try {
            //创建写文件对象
            RandomAccessFile raf_write = new RandomAccessFile(mergeFile, "rw");
            //遍历分块文件开始合并
            //读取文件缓冲区
            byte[] b = new byte[1024];
            for (File chunkFile : chunkFileList) {
                RandomAccessFile raf_read = new RandomAccessFile(chunkFile, "r");
                int len = -1;
                //读取分块文件
                while ((len = raf_read.read(b)) != -1) {
                    //向合并文件中写数据
                    raf_write.write(b, 0, len);
                }
                raf_read.close();
            }
            raf_write.close();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("merge file error:{}", e.getMessage());
            return null;
        }
        return mergeFile;
    }

    private boolean checkFileMd5(File mergeFile, String fileMd5) {
        if (mergeFile == null || StringUtils.isEmpty(fileMd5)) {
            return false;
        }
        //进行md5校验
        FileInputStream mergeFileInputStream = null;
        try {
            mergeFileInputStream = new FileInputStream(mergeFile);
            //得到文件的md5
            String mergeFileMd5 = DigestUtils.md5Hex(mergeFileInputStream);
            //比较md5
            if (fileMd5.equalsIgnoreCase(mergeFileMd5)) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("checkFileMd5 error,file is:{},md5 is: {}", mergeFile.getAbsoluteFile(), fileMd5);
        } finally {
            try {
                if (mergeFileInputStream != null) {
                    mergeFileInputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    //发送视频处理消息 (在合并chunks的时候也需要发送消息)
    public ResponseResult sendProcessVideoMsg(String mediaId) {

        //先验证一下在MongoDB中有没有
        Optional<MediaFile> optionalMediaFile = mediaFileRepository.findById(mediaId);
        if (!optionalMediaFile.isPresent()) {
            return new ResponseResult(CommonCode.FAIL);
        }
        //发送视频处理消息
        Map<String, String> msgMap = new HashMap<>();
        msgMap.put("mediaId", mediaId);
        String msg = JSON.toJSONString(msgMap); //发送的消息

        //向MQ发送消息
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EX_MEDIA_PROCESSTASK, routingkey_media_video,
                    msg);
            log.info("send media process task msg:{}", msg);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("send media process task error,msg is:{},error:{}", msg, e.getMessage());
            return new ResponseResult(CommonCode.FAIL);
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }
}
