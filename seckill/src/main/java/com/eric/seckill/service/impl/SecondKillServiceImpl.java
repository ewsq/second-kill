package com.eric.seckill.service.impl;

import com.eric.seckill.cache.anno.DisLock;
import com.eric.seckill.cache.anno.LogDetail;
import com.eric.seckill.cache.constant.CommonBizConstant;
import com.eric.seckill.common.bean.SkProject;
import com.eric.seckill.common.constant.ErrorCodeEnum;
import com.eric.seckill.common.exception.CustomException;
import com.eric.seckill.common.model.CommonResult;
import com.eric.seckill.service.SecondKillService;
import com.eric.seckill.service.SkGoodsSeckillService;
import com.eric.seckill.service.SkOrderService;
import com.eric.seckill.service.SkProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 秒杀的业务操作
 *
 * @author wang.js
 * @date 2019/1/3
 * @copyright yougou.com
 */
@Service
public class SecondKillServiceImpl implements SecondKillService {

	@Resource
	private SkProjectService skProjectService;

	@Resource
	private SkGoodsSeckillService skGoodsSeckillService;

	@Resource
	private SkOrderService skOrderService;

	private static final Logger LOGGER = LoggerFactory.getLogger(SecondKillServiceImpl.class);

	@Override
	@LogDetail
	@DisLock(key = "#userId", biz = CommonBizConstant.JOIN_SECOND_KILL)
	public CommonResult<Void> join(String projectId, String userId) {
		// 判断秒杀项目是否已经开始
		SkProject skProject = skProjectService.checkCanJoin(projectId);
		if (skProject == null) {
			throw new CustomException("获取秒杀项目失败, 请稍后再试");
		}
		// 判断用户是否已经秒杀到
		boolean result = skOrderService.countByUserIdAndGoodsId(userId, skProject.getGoodsId());
		if (result) {
			return CommonResult.fail("已经秒杀到该商品", ErrorCodeEnum.GOODS_KILLED.getErrCode());
		}
		// 根据项目id获取秒杀的库存
		long seckill = skGoodsSeckillService.seckill(skProject.getGoodsId());
		if (seckill >= 0) {
			// 存入订单系统
			LOGGER.info("用户{}秒杀到商品:{}", userId, skProject.getGoodsId());
			skOrderService.saveOrder(userId, skProject.getGoodsId());
			return CommonResult.success(null, null);
		}
		return CommonResult.fail("秒杀已结束", ErrorCodeEnum.SECKILL_END.getErrCode());
	}
}
