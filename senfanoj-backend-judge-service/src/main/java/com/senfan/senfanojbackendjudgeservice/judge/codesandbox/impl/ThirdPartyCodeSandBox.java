package com.senfan.senfanojbackendjudgeservice.judge.codesandbox.impl;


import com.senfan.senfanojbackendjudgeservice.judge.codesandbox.CodeSandBox;
import com.senfan.senfanojbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.senfan.senfanojbackendmodel.model.codesandbox.ExecuteCodeResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 第三方代码沙箱（调用网上线程的代码沙箱）
 */
@Slf4j
public class ThirdPartyCodeSandBox implements CodeSandBox {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest excuteCodeRequest) {
        log.info("第三方代码沙箱");
        return null;
    }
}
