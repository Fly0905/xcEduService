package com.xuecheng.manage_media_process.mq;


import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.MediaFileProcess_m3u8;
import com.xuecheng.framework.utils.HlsVideoUtil;
import com.xuecheng.framework.utils.Mp4VideoUtil;
import com.xuecheng.manage_media_process.dao.MediaFileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class MediaProcessTask {

    //ffmpeg绝对路径
    @Value("${xc-service-manage-media.ffmpeg-path}")
    String ffmpegPath;

    //上传文件根目录
    @Value("${xc-service-manage-media.video-location}")
    String serverPath;

    @Autowired
    MediaFileRepository mediaFileRepository;

    // 接受视频处理消息 进行 视频处理
    @RabbitListener(queues = "${xc-service-manage-media.mq.queue-media-video-processor}",
            containerFactory="customContainerFactory")
    public void receiveMediaProcessTask(String msg) {

        // 1、解析消息内容，得到mediaId
        Map msgMap = JSON.parseObject(msg, Map.class);
        log.info("receive media process task msg: {}", msgMap);

        // 2、拿mediaId从数据库查询文件信息
        //媒资文件id
        String mediaId = (String) msgMap.get("mediaId");
        //获取媒资文件信息
        Optional<MediaFile> optionalMediaFile = mediaFileRepository.findById(mediaId);
        if (!optionalMediaFile.isPresent()) {
            return;
        }
        MediaFile mediaFile = optionalMediaFile.get();
        //获取媒资文件类型
        String fileType = mediaFile.getFileType();
        if (!fileType.equals("avi")) {
            mediaFile.setProcessStatus("303004"); //无需处理的 数据字典
            mediaFileRepository.save(mediaFile); //保存到数据库
            return;
        }
        //需要处理
        mediaFile.setProcessStatus("303001");//处理中
        mediaFileRepository.save(mediaFile);

        // 3、使用工具类将avi文件生成mp4

        //(ffmpeg_path,  video_path,  mp4_name,  mp4folder_path)
        String video_path = serverPath + mediaFile.getFilePath() + mediaFile.getFileName();
        String mp4_name = mediaFile.getFileId() + ".mp4";
        String mp4folder_path = serverPath + mediaFile.getFilePath();
        Mp4VideoUtil mp4VideoUtil = new Mp4VideoUtil(ffmpegPath, video_path, mp4_name, mp4folder_path);
        String mp4Result = mp4VideoUtil.generateMp4();
        if (mp4Result == null || !mp4Result.equals("success")) {
            //操作失败写入处理日志
            setFailProcess(mp4Result, mediaFile);
            return;
        }

        // 4、将mp4生成m3u8和ts文件
        String mp4_video_path = serverPath + mediaFile.getFilePath() + mp4_name;//此地址为mp4的地址
        String m3u8_name = mediaFile.getFileId() + ".m3u8";
        String m3u8folder_path = serverPath + mediaFile.getFilePath() + "hls/";
        HlsVideoUtil hlsVideoUtil = new
                HlsVideoUtil(ffmpegPath, mp4_video_path, m3u8_name, m3u8folder_path);
        String tsResult = hlsVideoUtil.generateM3u8();

        if (tsResult == null || !tsResult.equals("success")) {
            setFailProcess(tsResult, mediaFile);
            return;
        }

        //处理成功，获取m3u8和ts文件列表
        List<String> ts_list = hlsVideoUtil.get_ts_list();
        mediaFile.setProcessStatus("303002");

        MediaFileProcess_m3u8 mediaFileProcess_m3u8 = new MediaFileProcess_m3u8();
        mediaFileProcess_m3u8.setTslist(ts_list);
        mediaFile.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
        String fileUrl = mediaFile.getFilePath() + "hls/" + m3u8_name; // 还有一个很重要的步骤,保存需要访问的fileUrl
        mediaFile.setFileUrl(fileUrl);
        mediaFileRepository.save(mediaFile);
    }

    //操作失败写入处理日志
    private void setFailProcess(String result, MediaFile mediaFile){
        mediaFile.setProcessStatus("303003");//处理状态为处理失败
        MediaFileProcess_m3u8 mediaFileProcess_m3u8 = new MediaFileProcess_m3u8();
        mediaFileProcess_m3u8.setErrormsg(result);
        mediaFile.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
        mediaFileRepository.save(mediaFile);
    }
}
