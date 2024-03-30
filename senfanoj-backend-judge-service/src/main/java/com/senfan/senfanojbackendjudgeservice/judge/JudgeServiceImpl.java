package com.senfan.senfanojbackendjudgeservice.judge;

import cn.hutool.json.JSONUtil;
import com.senfan.senfanojbackendcommon.common.ErrorCode;
import com.senfan.senfanojbackendcommon.exception.BusinessException;
import com.senfan.senfanojbackendjudgeservice.judge.codesandbox.CodeSandBox;
import com.senfan.senfanojbackendjudgeservice.judge.codesandbox.CodeSandBoxFactory;
import com.senfan.senfanojbackendjudgeservice.judge.codesandbox.CodeSandBoxProxy;
import com.senfan.senfanojbackendjudgeservice.judge.strategy.JudgeContext;
import com.senfan.senfanojbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.senfan.senfanojbackendmodel.model.codesandbox.ExecuteCodeResponse;
import com.senfan.senfanojbackendmodel.model.codesandbox.JudgeInfo;
import com.senfan.senfanojbackendmodel.model.dto.question.JudgeCase;
import com.senfan.senfanojbackendmodel.model.entity.Question;
import com.senfan.senfanojbackendmodel.model.entity.QuestionSubmit;
import com.senfan.senfanojbackendmodel.model.enums.JudgeInfoMessageEnum;
import com.senfan.senfanojbackendmodel.model.enums.QuestionSubmitStatusEnum;
import com.senfan.senfanojbackendserviceclient.service.QuestionFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JudgeServiceImpl implements JudgeService {

    @Resource
    QuestionFeignClient questionFeignClient;


    @Resource
    private JudgeManager judgeManager;

    @Value("${codesandbox.type:example}")
    private String type;

    @Override
    public QuestionSubmit doJudge(long questionSubmitId) {
        // 1)传入题目的提交id，获取到对应的题目、提交信息(包含代码、编程语言等)
        QuestionSubmit questionSubmit = questionFeignClient.getQuestionSubmitById(questionSubmitId);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交信息不存在");
        }
        Long questionId = questionSubmit.getQuestionId();
        Question question = questionFeignClient.getQuestionById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        String language = questionSubmit.getLanguage();
        String code = questionSubmit.getCode();
        Integer status = questionSubmit.getStatus();
        // 2)如果题目提交状态不为等待中，就不用重复执行了
        if (!questionSubmit.getStatus().equals(QuestionSubmitStatusEnum.WAITING.getValue())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目正在判断中");
        }
        // 3)更改判题(题目提交）的状态为“判题中”，防止重复执行，也能让用户即时看到状态
        QuestionSubmit questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.RUNNING.getValue());
        boolean update = questionFeignClient.updateQuestionSubmitById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新题目状态失败");
        }
        // 4)调用沙箱，获取到执行结果
        CodeSandBox codeSandBox = CodeSandBoxFactory.newInstance(type);
        codeSandBox = new CodeSandBoxProxy(codeSandBox);
        // 获取输入用例
        String judgeCaseStr = question.getJudgeCase();
        List<JudgeCase> judgeCaseList = JSONUtil.toList(judgeCaseStr, JudgeCase.class);
        List<String> inputList = judgeCaseList.stream().map(JudgeCase::getInput).collect(Collectors.toList());
        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                .inputList(inputList)
                .language(language)
                .code(code)
                .build();
        ExecuteCodeResponse executeCodeResponse = null;
        try {
            executeCodeResponse = codeSandBox.executeCode(executeCodeRequest);
        } catch (Exception e) {
            log.info("执行代码沙箱失败",e);
            QuestionSubmit questionSubmitOld = new QuestionSubmit();
            JudgeInfo judgeInfo = new JudgeInfo();
            judgeInfo.setMessage(JudgeInfoMessageEnum.COMPILE_ERROR.getValue());
            String jsonStr = JSONUtil.toJsonStr(judgeInfo);
            questionSubmitOld.setId(questionSubmitId);
            questionSubmitOld.setJudgeInfo(jsonStr);
            questionSubmitOld.setStatus(2);
            questionFeignClient.updateQuestionSubmitById(questionSubmitOld);
            throw new BusinessException(ErrorCode.PARAMS_ERROR,e.getMessage());
        }
        List<String> outputList = executeCodeResponse.getOutputList();
        // 5)根据沙箱的执行结果，设置题目的判题状态和信息
        JudgeContext judgeContext = new JudgeContext();
        judgeContext.setJudgeInfo(executeCodeResponse.getJudgeInfo());
        judgeContext.setInputList(inputList);
        judgeContext.setOutputList(outputList);
        judgeContext.setJudgeCaseList(judgeCaseList);
        judgeContext.setQuestion(question);
        judgeContext.setQuestionSubmit(questionSubmit);
        JudgeInfo judgeInfo = judgeManager.doJudge(judgeContext);
        // 6）修改数据库中的判题结果
        questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
        questionSubmitUpdate.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));
        update = questionFeignClient.updateQuestionSubmitById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新错误");
        }
        QuestionSubmit questionSubmitResult = questionFeignClient.getQuestionSubmitById(questionSubmitId);
        return questionSubmitResult;
    }
}
