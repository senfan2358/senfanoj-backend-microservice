
package com.senfan.senfanojcodesandbox.controller;

import com.senfan.senfanojbackendcommon.utils.SM2Utils;
import com.senfan.senfanojcodesandbox.codesandbox.JavaDockerCodeSandbox;
import com.senfan.senfanojcodesandbox.codesandbox.JavaNativeCodeSandbox;
import com.senfan.senfanojcodesandbox.model.ExecuteCodeRequest;
import com.senfan.senfanojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@RestController("/")
public class MainController {

    // 定义鉴权请求头和密钥
    private static final String AUTH_REQUEST_HEADER = "auth";

    private static final String AUTH_REQUEST_PRIVATE_KEY = "84790929baa920b10555c4095764476229114cf89cf2d344778710286c9f9195";

    @Resource
    private JavaNativeCodeSandbox javaNativeCodeSandbox;
    @Resource
    private JavaDockerCodeSandbox javaDockerCodeSandbox;

    @GetMapping("/health")
    public String healthCheck() {
        return "ok";
    }

    /**
     * 执行代码
     *
     * @param executeCodeRequest
     * @return
     */
    @PostMapping("/executeCode")
    public ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest request,
                                    HttpServletResponse response) {
        // 基本的认证
        String authHeader = request.getHeader(AUTH_REQUEST_HEADER);
        if (!SM2Utils.verify(AUTH_REQUEST_PRIVATE_KEY,"senfan235",authHeader)) {
            response.setStatus(403);
            return null;
        }
        if (executeCodeRequest == null) {
            response.setStatus(400);
            return null;
        }
        return javaNativeCodeSandbox.executeCode(executeCodeRequest);
    }

    @GetMapping("/test")
    public ExecuteCodeResponse test(){
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        List<String> inputList = new ArrayList<>();
        inputList.add("1");
        inputList.add("2");
        String code = "public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        int a = Integer.parseInt(args[0]);\n" +
                "        int b = Integer.parseInt(args[1]);\n" +
                "        System.out.println((a + b));\n" +
                "    }\n" +
                "}";
        executeCodeRequest.setInputList(inputList);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        return javaDockerCodeSandbox.executeCode(executeCodeRequest);
    }
}