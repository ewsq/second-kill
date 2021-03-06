package com.eric.user.service.base;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eric.seckill.cache.anno.ParamCheck;
import com.eric.seckill.common.constant.UserStatus;
import com.eric.seckill.common.exception.CustomException;
import com.eric.seckill.common.model.CommonResult;
import com.eric.seckill.common.model.feign.ChangePointRequest;
import com.eric.seckill.common.utils.SignUtil;
import com.eric.user.bean.UserInfo;
import com.eric.user.bean.UserLevelDetail;
import com.eric.user.bean.UserMaster;
import com.eric.user.bean.UserPointLog;
import com.eric.user.constant.ErrorCodeEnum;
import com.eric.user.dao.UserInfoMapper;
import com.eric.user.model.*;
import com.eric.user.service.*;
import org.apache.commons.lang3.StringUtils;
import org.dozer.DozerBeanMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

/**
 * 用户信息
 *
 * @author wang.js on 2019/1/16.
 * @version 1.0
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo>
		implements UserInfoService, BaseService {

	@Resource
	private UserLevelDetailService userLevelDetailService;

	@Resource
	private UserMasterService userMasterService;

	@Resource
	private DozerBeanMapper dozerBeanMapper;

	@Resource
	private UserPointLogService userPointLogService;

	@Value("${user.charge.secret}")
	private String appSecret;

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
	public int insert(UserMaster userMaster, String mobile) {
		UserInfo t = new UserInfo().setMobilePhone(mobile)
				.setRegisterTime(new Date()).setUserId(String.valueOf(userMaster.getUserId()))
				.setUserInfoId(UUID.randomUUID().toString())
				.setUserLevel(userLevelDetailService.findBaseLevel());
		initCreateTime(t);
		// 保存用户信息
		int result = baseMapper.insert(t);
		if (result == 0) {
			throw new CustomException("保存用户信息失败");
		}
		return result;
	}

	@Override
	public Integer checkMobileExist(String phone) {
		return baseMapper.selectCount(new QueryWrapper<UserInfo>().eq("mobile_phone", phone));
	}

	@Override
	@ParamCheck
	public CommonResult<Void> updateUserInfo(UserInfoModifyRequest userInfo) {
		if (userInfo == null || StringUtils.isBlank(userInfo.getUserId())) {
			return CommonResult.fail(ErrorCodeEnum.EMPTY_USER_ID.getErrorMsg(), ErrorCodeEnum.EMPTY_USER_ID.getErrorCode());
		}
		// 判断用户id是否存在
		UserMaster user = userMasterService.findUserMasterByUserId(userInfo.getUserId());
		if (user == null) {
			return CommonResult.fail(ErrorCodeEnum.USER_NOT_FOUND.getErrorMsg(), ErrorCodeEnum.USER_NOT_FOUND.getErrorCode());
		}
		if (UserStatus.ACTIVE.getStatusCode().equals(user.getUserStats())) {
			UserInfo t = new UserInfo();
			dozerBeanMapper.map(userInfo, t);
			t.setUpdateTime(new Date());
			t.setUserId(null);
			int effect = baseMapper.update(t, new QueryWrapper<UserInfo>().eq("user_id", userInfo.getUserId()));
			if (effect > 0) {
				return CommonResult.success(null, ErrorCodeEnum.UPDATE_SUCCESS.getErrorMsg());
			}
			return CommonResult.fail(ErrorCodeEnum.UPDATE_FAIL.getErrorMsg(), ErrorCodeEnum.UPDATE_FAIL.getErrorCode());
		}
		return CommonResult.fail(ErrorCodeEnum.UPDATE_FORBIDDEN.getErrorMsg(), ErrorCodeEnum.UPDATE_FORBIDDEN.getErrorCode());
	}

	@Override
	public ChargeBalanceResponse updateUserBalance(ChargeBalanceRequest request) {
		String userInfoId = baseMapper.findUserInfoIdByUserId(request.getChargeUserId());
		Integer balance = baseMapper.findUserBalanceByUserInfoId(userInfoId);
		balance = balance == null ? 0 : balance;
		int finalBalance = BigDecimal.valueOf(balance).add(new BigDecimal(request.getChargeAmount())).intValue();
		if (finalBalance < 0) {
			throw new CustomException(ErrorCodeEnum.BALANCE_NOT_ENOUGH.getErrorMsg());
		}
		int effect = baseMapper.updateUserBalance(userInfoId, request.getChargeAmount());
		if (effect == 0) {
			throw new CustomException(ErrorCodeEnum.UPDATE_FAIL.getErrorMsg());
		}
		return new ChargeBalanceResponse().setUserBalance(String.valueOf(finalBalance));
	}

	@Override
	public UserPointChangeResponse updateUserPoint(UserPointChangeRequest request) {
		String userInfoId = baseMapper.findUserInfoIdByUserId(request.getUserId());
		Integer point = baseMapper.findUserPointByUserInfoId(userInfoId);
		int finalPoint = BigDecimal.valueOf(point).add(new BigDecimal(request.getChangePoint())).intValue();
		if (finalPoint < 0) {
			throw new CustomException(ErrorCodeEnum.BALANCE_NOT_ENOUGH.getErrorMsg());
		}
		int effect = baseMapper.updateUserPoint(userInfoId, request.getChangePoint(), null);
		if (effect == 0) {
			throw new CustomException(ErrorCodeEnum.UPDATE_FAIL.getErrorMsg());
		}
		return new UserPointChangeResponse().setFinalPoint(String.valueOf(finalPoint));
	}

	@Override
	@ParamCheck
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
	public CommonResult<Void> changePoint(ChangePointRequest request) {
		// 校验签名
		boolean verify = SignUtil.verify(request, request.getSign(), appSecret);
		if (!verify) {
			return CommonResult.fail(ErrorCodeEnum.ERROR_SIGN.getErrorMsg(), ErrorCodeEnum.ERROR_SIGN.getErrorCode());
		}
		String userStats = userMasterService.findUserStatsByUserId(request.getUserId());
		if (StringUtils.isBlank(userStats)) {
			return CommonResult.fail(ErrorCodeEnum.USER_NOT_FOUND.getErrorMsg(), ErrorCodeEnum.USER_NOT_FOUND.getErrorCode());
		}
		if (!UserStatus.ACTIVE.getStatusCode().equals(userStats)) {
			return CommonResult.fail(ErrorCodeEnum.USER_ABNORMAL.getErrorMsg(), ErrorCodeEnum.USER_ABNORMAL.getErrorCode());
		}
		// 判断积分变动是否已经处理过
		Integer integer = userPointLogService.checkExistByOutTradeNo(request.getReferNumber());
		if (integer != null && integer > 0) {
			return CommonResult.fail(ErrorCodeEnum.DUPLICATE.getErrorMsg(), ErrorCodeEnum.DUPLICATE.getErrorCode());
		}
		UserInfo userInfo = this.baseMapper.selectOne(new QueryWrapper<UserInfo>().eq("user_id", request.getUserId()));
		// 改动后的积分是否小于0
		Integer originalUserPoint = this.baseMapper.findUserPointByUserInfoId(userInfo.getUserInfoId());
		int lastPoint = new BigDecimal(originalUserPoint).add(new BigDecimal(request.getChangePoint())).intValue();
		if (lastPoint < 0) {
			return CommonResult.fail(ErrorCodeEnum.POINT_NOT_ENOUGH.getErrorMsg(), ErrorCodeEnum.POINT_NOT_ENOUGH.getErrorCode());
		}
		// 保存积分变动记录, 修改最终积分
		UserPointLog log = new UserPointLog().setUserId(request.getUserId()).setReferNumber(request.getReferNumber())
				.setPointSource(request.getPointSource()).setId(UUID.randomUUID().toString()).setCreateTime(new Date())
				.setChangePoint(request.getChangePoint());
		userPointLogService.insert(log);
		// 判断用户的等级是否有变动
		UserLevelDetail currentLevel = userLevelDetailService.findMinPointByUserLevelId(userInfo.getUserLevel());
		String userLevelDetailId = null;
		if (lastPoint > currentLevel.getMaxPoint()) {
			// 需要升级
			userLevelDetailId = userLevelDetailService.findLevelDetailId(lastPoint);
		}
		// 修改最终积分
		baseMapper.updateUserPoint(userInfo.getUserInfoId(), request.getChangePoint(), userLevelDetailId);
		return CommonResult.success(null, ErrorCodeEnum.UPDATE_SUCCESS.getErrorMsg());
	}

	@Override
	public CommonResult<String> findUserLevelIdByUserId(String userId) {
		return CommonResult.success(this.baseMapper.findUserLevelIdByUserId(userId), null);
	}
}
