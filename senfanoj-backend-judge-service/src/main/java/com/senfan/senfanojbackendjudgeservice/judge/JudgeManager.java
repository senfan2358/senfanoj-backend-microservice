package com.senfan.senfanojbackendjudgeservice.judge;

import com.senfan.senfanojbackendjudgeservice.judge.strategy.DefaultJudgeStrategy;
import com.senfan.senfanojbackendjudgeservice.judge.strategy.JavaLanguageJudgeStrategy;
import com.senfan.senfanojbackendjudgeservice.judge.strategy.JudgeContext;
import com.senfan.senfanojbackendjudgeservice.judge.strategy.JudgeStrategy;
import com.senfan.senfanojbackendmodel.model.codesandbox.JudgeInfo;
import com.senfan.senfanojbackendmodel.model.entity.QuestionSubmit;
import org.springframework.stereotype.Service;

/**
 * 判题管理（简化调用）
 */
@Service
public class JudgeManager {

    /**
     * 执行判题
     *
     * @param judgeContext
     * @return
     */
    JudgeInfo doJudge(JudgeContext judgeContext) {
        QuestionSubmit questionSubmit = judgeContext.getQuestionSubmit();
        String language = questionSubmit.getLanguage();
        JudgeStrategy judgeStrategy = new DefaultJudgeStrategy();
        if ("java".equals(language)) {
            judgeStrategy = new JavaLanguageJudgeStrategy();
        }
        return judgeStrategy.doJudge(judgeContext);
    }

}
