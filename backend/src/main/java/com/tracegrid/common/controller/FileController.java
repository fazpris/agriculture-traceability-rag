package com.tracegrid.common.controller;

import cn.hutool.core.util.IdUtil;
import com.tracegrid.common.exception.BusinessException;
import com.tracegrid.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

@Tag(name = "文件上传")
@RestController
@RequestMapping("/file")
public class FileController {

    @Value("${file.upload-path:./uploads}")
    private String uploadPath;

    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public R<String> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.of("请选择要上传的文件");
        }

        String originalName = file.getOriginalFilename();
        String ext = StringUtils.getFilenameExtension(originalName);
        String fileName = IdUtil.fastSimpleUUID() + (StringUtils.hasText(ext) ? "." + ext : "");
        String dateDir = LocalDate.now().toString().replace("-", "");

        try {
            Path dir = Paths.get(uploadPath, dateDir);
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName);
            file.transferTo(target);
            return R.ok("/api/file/preview/" + dateDir + "/" + fileName);
        } catch (IOException e) {
            throw BusinessException.of("文件上传失败");
        }
    }
}
