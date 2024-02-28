package com.senfan.senfanojbackendmodel.model.codesandbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeRequest {
    /**
     * 输入样例
     */
    List<String> inputList;

    /**
     * 代码
     */
    private String code;

    /**
     * 编程语言
     */
    private String language;
}
