package com.it.gmall.manager.controller;

import org.apache.commons.lang3.StringUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@CrossOrigin
public class FileUploadController {

    @Value("${fileServer.url}")
    String fileUrl;

    @RequestMapping("/fileUpload")
    public String textFileUpload(MultipartFile file) throws IOException, MyException {
        String url = fileUrl;
        if(file != null){
            String confire = this.getClass().getResource("/tracker.conf").getFile();
            ClientGlobal.init(confire);
            TrackerClient trackerClient=new TrackerClient();
            //获取连接
            TrackerServer trackerServer=trackerClient.getConnection();
            StorageClient storageClient=new StorageClient(trackerServer,null);

            //获取文件名
            String orginalFilename=file.getOriginalFilename();
            //获取文件后缀
            String extName = StringUtils.substringAfterLast(orginalFilename,".");
            String[] upload_file = storageClient.upload_file(file.getBytes(), extName, null);
            for (int i = 0; i < upload_file.length; i++) {
                String s = upload_file[i];
                url += "/"+s;
                System.out.println("s = " + s);
            }
        }
        return url;
    }
}
