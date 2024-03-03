package com.senfan.senfanojcodesandbox.codesandbox;

import com.senfan.senfanojbackendcommon.common.ErrorCode;
import com.senfan.senfanojbackendcommon.exception.BusinessException;
import com.senfan.senfanojcodesandbox.model.ExecuteCodeRequest;
import com.senfan.senfanojcodesandbox.model.ExecuteCodeResponse;
import com.senfan.senfanojcodesandbox.model.ExecuteMessage;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * 反射实现
 */
@Slf4j
public class JavaReflectSandBox extends JavaCodeSandboxTemplate {
    private static final long TIME_OUT = 5000L;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        // 1. 把用户的代码保存为文件
        File userCodeFile = saveCodeToFile(code);

        // 2. 编译代码，得到 class 文件
        ExecuteMessage compileFileExecuteMessage = compileFile(userCodeFile);
        System.out.println(compileFileExecuteMessage);

        // 3. 执行代码，得到输出结果
        List<ExecuteMessage> executeMessageList = runFileReflect(userCodeFile, inputList);

        // 4. 收集整理输出结果
        ExecuteCodeResponse outputResponse = getOutputResponse(executeMessageList);

        // 5. 文件清理
        boolean b = deleteFile(userCodeFile);
        if (!b) {
            log.error("deleteFile error, userCodeFilePath = {}", userCodeFile.getAbsolutePath());
        }
        return outputResponse;
    }

    public List<ExecuteMessage> runFileReflect(File userCodeFile, List<String> inputList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        Class<?> mainClass = null;
        Method[] methods= new Method[0];

        try {
            // 创建URLClassLoader，用于加载指定路径下的类文件
            URLClassLoader classLoader = new URLClassLoader(new URL[]{new URL("file://" + userCodeParentPath)});

            // 加载目标类
            mainClass = classLoader.loadClass("Solution");

            // 获取 solution 方法
            methods = mainClass.getDeclaredMethods();
        } catch (Exception e) {
            log.info("获取方法失败",e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"获取方法失败");
        }
        Method aimMethod = null;
        for (Method method : methods) {
            // Check if the method name is "solution"
            if (method.getName().equals("solution")) {
                aimMethod = method;
                break;
            }
        }
        if (aimMethod == null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"获取方法失败");
        }
        List<ExecuteMessage> executeMessageList = invokeReflectMethod(aimMethod,inputList);
        return executeMessageList;
    }

    private List<ExecuteMessage> invokeReflectMethod(Method aimMethod, List<String> inputList) {

        return null;
    }
}
