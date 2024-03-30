package com.senfan.senfanojbackendjudgeservice.judge.codesandbox.impl;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.senfan.senfanojbackendcommon.common.ErrorCode;
import com.senfan.senfanojbackendcommon.exception.BusinessException;
import com.senfan.senfanojbackendcommon.utils.SM2Utils;
import com.senfan.senfanojbackendjudgeservice.judge.codesandbox.CodeSandBox;
import com.senfan.senfanojbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.senfan.senfanojbackendmodel.model.codesandbox.ExecuteCodeResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 远程代码沙箱（实际调用接口的沙箱）
 */
@Slf4j
public class RemoteCodeSandBox implements CodeSandBox {

    // 定义鉴权请求头和密钥
    private static final String AUTH_REQUEST_HEADER = "auth";

    private static final String AUTH_REQUEST_PUBLIC_KEY = "04e8bdc14567991868c120841cfe9b9394e571abd11920888e686445f8d5480c538574b2d9d72010526441f11c4ec7c8d1b8ba8bd6a45f391a043a88b32af4aad0";


    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        log.info("远程代码沙箱");
        String url = "http://localhost:8090/executeCode";
        String json = JSONUtil.toJsonStr(executeCodeRequest);
        HttpResponse response = HttpUtil.createPost(url)
                .header(AUTH_REQUEST_HEADER, SM2Utils.encrypt(AUTH_REQUEST_PUBLIC_KEY,"senfan235"))
                .body(json)
                .execute();
        int status = response.getStatus();
        if (status != 200){
            if (status == 400){
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }
            if (status == 403){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"编译错误");
        }
        String responseStr = response.body();
        if (StringUtils.isBlank(responseStr)) {
            throw new BusinessException(ErrorCode.API_REQUEST_ERROR, "executeCode remoteSandbox error, message = " + responseStr);
        }
        return JSONUtil.toBean(responseStr, ExecuteCodeResponse.class);
    }
}
