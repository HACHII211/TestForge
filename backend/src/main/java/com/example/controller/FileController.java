package com.example.controller;

import cn.hutool.core.io.FileUtil;
import com.example.common.Result;
import com.example.exception.CustomException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;


@RestController
@RequestMapping("/files")
public class FileController {

    private static final String filePath = System.getProperty("user.dir") + "/files/";


    @PostMapping("/upload")
    public Result upload(MultipartFile file){
        String originalFilename = file.getOriginalFilename();

        if (!FileUtil.isDirectory(filePath)) {
            FileUtil.mkdir(filePath);
        }

        String fileName = System.currentTimeMillis() + "_" + originalFilename;
        String realPath = filePath + fileName;

        try {
            FileUtil.writeBytes(file.getBytes(),realPath);
        } catch (IOException e){
            throw new CustomException("500","上传失败");
        }

        String url = "http://localhost:9090/files/download/"+ fileName;
        return Result.success(url);
    }

    @GetMapping("/download/{fileName}")
    public void download(@PathVariable String fileName, HttpServletResponse response)  {
        try {
            response.addHeader("Content-Disposition","attachment:filename="+ URLEncoder.encode(fileName, StandardCharsets.UTF_8));
            response.setContentType("application/octet-stream");
            OutputStream os = response.getOutputStream();
            String realPath = filePath + fileName;
            byte[] bytes = FileUtil.readBytes(realPath);
            os.write(bytes);
            os.flush();
            os.close();
        } catch (IOException e){
            e.printStackTrace();
            throw new CustomException("500","文件下载失败");
        }
    }

    @PostMapping("/wang/upload")
    public Map<String, Object> wangEditorUpload(MultipartFile file){
        String originalFilename = file.getOriginalFilename();

        if (!FileUtil.isDirectory(filePath)) {
            FileUtil.mkdir(filePath);
        }

        String fileName = System.currentTimeMillis() + "_" + originalFilename;
        String realPath = filePath + fileName;

        try {
            FileUtil.writeBytes(file.getBytes(),realPath);
        } catch (IOException e){
            throw new CustomException("500","上传失败");
        }

        String url = "http://localhost:9090/files/download/"+ fileName;

        Map<String, Object> resMap = new HashMap<>();
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> urlMap = new HashMap<>();
        urlMap.put("url",url);
        list.add(urlMap);
        resMap.put("errno", 0);
        resMap.put("data", list);
        return resMap;

    }

}
