package com.eric.user.service.base;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eric.user.bean.UserLevelDetail;
import com.eric.user.dao.UserLevelDetailMapper;
import com.eric.user.service.UserLevelDetailService;
import org.springframework.stereotype.Service;

/**
 * 用户等级明细
 * @author wang.js on 2019/1/16.
 * @version 1.0
 */
@Service
public class UserLevelDetailServiceImpl extends ServiceImpl<UserLevelDetailMapper, UserLevelDetail> implements UserLevelDetailService {
}