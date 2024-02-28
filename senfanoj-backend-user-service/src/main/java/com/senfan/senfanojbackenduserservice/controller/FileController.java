package com.senfan.senfanojbackenduserservice.controller;

import cn.hutool.core.io.FileUtil;
import com.senfan.senfanojbackendcommon.common.BaseResponse;
import com.senfan.senfanojbackendcommon.common.ErrorCode;
import com.senfan.senfanojbackendcommon.common.ResultUtils;
import com.senfan.senfanojbackendcommon.constant.FileConstant;
import com.senfan.senfanojbackendcommon.exception.BusinessException;
import com.senfan.senfanojbackendmodel.model.dto.file.UploadFileRequest;
import com.senfan.senfanojbackendmodel.model.entity.User;
import com.senfan.senfanojbackendmodel.model.enums.FileUploadBizEnum;
import com.senfan.senfanojbackenduserservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Arrays;

/**
 * 文件接口
 */
@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Resource
    private UserService userService;


    /**
     * 文件上传
     *
     * @param multipartFile
     * @param uploadFileRequest
     * @param request
     * @return
     */
    @PostMapping("/upload")
    public BaseResponse<String> uploadFile(@RequestPart("file") MultipartFile multipartFile,
                                           UploadFileRequest uploadFileRequest, HttpServletRequest request) {
        String biz = uploadFileRequest.getBiz();
        FileUploadBizEnum fileUploadBizEnum = FileUploadBizEnum.getEnumByValue(biz);
        if (fileUploadBizEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        validFile(multipartFile, fileUploadBizEnum);
        User loginUser = userService.getLoginUser(request);
        // 文件目录：根据业务、用户来划分
        String uuid = RandomStringUtils.randomAlphanumeric(8);
        String filename = uuid + "-" + multipartFile.getOriginalFilename();
        // String filepath = String.format("/%s/%s/%s", fileUploadBizEnum.getValue(), loginUser.getId(), filename);
        // 获取 Spring 应用程序的根目录路径

        String imagePath = null;
        try {
            // 获取 Spring 应用程序的根目录路径
            imagePath = ResourceUtils.getURL("classpath:").getPath() + "static/images/";
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "资源路径不存在");
        }
        File file = new File(imagePath + filename);
        try {
            // 上传文件
            multipartFile.transferTo(file);
            // cosManager.putObject(filepath, file);
            // 返回可访问地址
            return ResultUtils.success(FileConstant.IMAGE_HOST + filename);
        } catch (Exception e) {
            log.error("file upload error, filepath = " + filename, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        }
        // finally {
        //     if (file != null) {
        //         // 删除临时文件
        //         boolean delete = file.delete();
        //         if (!delete) {
        //             log.error("file delete error, filepath = {}", filename);
        //         }
        //     }
        // }
    }

    /**
     * 校验文件
     *
     * @param multipartFile
     * @param fileUploadBizEnum 业务类型
     */
    private void validFile(MultipartFile multipartFile, FileUploadBizEnum fileUploadBizEnum) {
        // 文件大小
        long fileSize = multipartFile.getSize();
        // 文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        final long ONE_M = 1024 * 1024L;
        if (FileUploadBizEnum.USER_AVATAR.equals(fileUploadBizEnum)) {
            if (fileSize > ONE_M) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 1M");
            }
            if (!Arrays.asList("jpeg", "jpg", "svg", "png", "webp").contains(fileSuffix)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
        }
    }
}
