package com.senfan.senfanojbackendquestionservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.senfan.senfanojbackendcommon.common.ErrorCode;
import com.senfan.senfanojbackendcommon.constant.CommonConstant;
import com.senfan.senfanojbackendcommon.exception.BusinessException;
import com.senfan.senfanojbackendcommon.utils.SqlUtils;
import com.senfan.senfanojbackendmodel.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.senfan.senfanojbackendmodel.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.senfan.senfanojbackendmodel.model.entity.Question;
import com.senfan.senfanojbackendmodel.model.entity.QuestionSubmit;
import com.senfan.senfanojbackendmodel.model.entity.User;
import com.senfan.senfanojbackendmodel.model.enums.QuestionSubmitLanguageEnum;
import com.senfan.senfanojbackendmodel.model.enums.QuestionSubmitStatusEnum;
import com.senfan.senfanojbackendmodel.model.vo.QuestionSubmitVO;
import com.senfan.senfanojbackendquestionservice.mapper.QuestionSubmitMapper;
// import com.senfan.senfanojbackendquestionservice.rabbitmq.MyMessageProducer;
import com.senfan.senfanojbackendquestionservice.rabbitmq.MyMessageProducer;
import com.senfan.senfanojbackendquestionservice.service.QuestionService;
import com.senfan.senfanojbackendquestionservice.service.QuestionSubmitService;
import com.senfan.senfanojbackendserviceclient.service.JudgeFeignClient;
import com.senfan.senfanojbackendserviceclient.service.UserFeignClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 *
 */
@Service
public class QuestionSubmitServiceImpl extends ServiceImpl<QuestionSubmitMapper, QuestionSubmit>
        implements QuestionSubmitService {
    @Resource
    QuestionService questionService;

    @Resource
    UserFeignClient userService;

    @Resource
    MyMessageProducer myMessageProducer;
    @Lazy
    @Resource
    JudgeFeignClient judgeService;

    /**
     * 提交题目
     *
     * @param questionSubmitAddRequest
     * @param loginUser
     * @return
     */
    @Override
    public long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, User loginUser) {
        // 校验编程语言是否合法
        String language = questionSubmitAddRequest.getLanguage();
        QuestionSubmitLanguageEnum enumByValue = QuestionSubmitLanguageEnum.getEnumByValue(language);
        if (enumByValue == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编程语言错误");
        }
        Long questionId = questionSubmitAddRequest.getQuestionId();
        // 判断实体是否存在，根据类型获取实体
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 是否已提交题目
        Long userId = loginUser.getId();
        // 每个用户串行提交题目
        QuestionSubmit questionSubmit = new QuestionSubmit();
        questionSubmit.setUserId(userId);
        questionSubmit.setQuestionId(questionId);
        questionSubmit.setCode(questionSubmitAddRequest.getCode());
        questionSubmit.setLanguage(language);
        // 设置初始状态
        questionSubmit.setStatus(QuestionSubmitStatusEnum.WAITING.getValue());
        questionSubmit.setJudgeInfo("{}");
        boolean save = this.save(questionSubmit);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据插入失败");
        }
        Long questionSubmitId = questionSubmit.getId();
        // 执行判题服务
        myMessageProducer.sendMessage("code_exchange", "my_routingKey", String.valueOf(questionSubmitId));

        // CompletableFuture.runAsync(() -> {
        //     judgeService.doJudge(questionSubmitId);
        // });
        return questionSubmitId;
    }

    @Override
    public QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitQueryRequest) {
        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
        if (questionSubmitQueryRequest == null) {
            return queryWrapper;
        }
        String language = questionSubmitQueryRequest.getLanguage();
        Integer status = questionSubmitQueryRequest.getStatus();
        Long questionId = questionSubmitQueryRequest.getQuestionId();
        Long userId = questionSubmitQueryRequest.getUserId();
        String sortField = questionSubmitQueryRequest.getSortField();
        String sortOrder = questionSubmitQueryRequest.getSortOrder();
        // 拼接查询条件
        queryWrapper.eq(StringUtils.isNotBlank(language), "language", language);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionId), "questionId", questionId);
        queryWrapper.eq(QuestionSubmitStatusEnum.getEnumByValue(status) != null, "status", status);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public QuestionSubmitVO getQuestionSubmitVO(QuestionSubmit questionSubmit, User loginUser) {
        QuestionSubmitVO questionSubmitVO = QuestionSubmitVO.objToVo(questionSubmit);
        // 脱敏，仅本人和管理员能看见自己（提交userId 和 登录用户 id 不同）提交的代码
        Long userId = loginUser.getId();
        // 处理脱敏
        if (!questionSubmit.getUserId().equals(userId) && !userService.isAdmin(loginUser)) {
            questionSubmitVO.setCode(null);
        }
        return questionSubmitVO;
    }

    @Override
    public Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> questionSubmitPage, User loginUser) {
        List<QuestionSubmit> questionSubmitList = questionSubmitPage.getRecords();
        Page<QuestionSubmitVO> questionSubmitVOPage = new Page<>(questionSubmitPage.getCurrent(), questionSubmitPage.getSize(), questionSubmitPage.getTotal());
        if (CollectionUtils.isEmpty(questionSubmitList)) {
            return questionSubmitVOPage;
        }
        List<QuestionSubmitVO> questionSubmitVOList = questionSubmitList.stream().
                map(questionSubmit -> getQuestionSubmitVO(questionSubmit, loginUser))
                .collect(Collectors.toList());
        questionSubmitVOPage.setRecords(questionSubmitVOList);
        return questionSubmitVOPage;
    }
}




