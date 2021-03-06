package com.eric.order.controller;

import com.eric.order.feign.UserMasterFeign;
import com.eric.order.model.*;
import com.eric.order.service.*;
import com.eric.seckill.cache.anno.LogDetail;
import com.eric.seckill.common.model.CommonResult;
import com.eric.seckill.common.model.feign.ReceiveOrderRequest;
import com.eric.seckill.common.model.feign.ShippingRequest;
import com.eric.seckill.common.model.feign.UserQueryRequest;
import com.eric.seckill.common.model.feign.UserQueryResponse;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 订单相关的接口
 *
 * @author Eric on 2019/1/24.
 * @version 1.0
 */
@RequestMapping("/api")
@RestController
public class OrderController {

	@Resource
	private UserMasterFeign feign;

	@Resource
	private CreateOrderService createOrderService;

	@Resource
	private UseCouponService useCouponService;

	@Resource
	private PayingOrderService payingOrderService;

	@Resource
	private OrderSuccessService orderSuccessService;

	@Resource
	private OrderQueryService orderQueryService;

	@Resource
	private ShippingOrderService shippingOrderService;

	@Resource
	private ReceiveOrderService receiveOrderService;

	/**
	 * 测试分布式主键的生成
	 *
	 * @return
	 */
	@GetMapping("/generateId")
	public String generateId() {
		long id = createOrderService.generateId();
		if (id % 2 == 0) {
			System.out.println("生成的id:" + id + "是偶数");
		} else {
			System.out.println("生成的id:" + id + "是奇数");
		}
		return String.valueOf(id);
	}

	/**
	 * 创建订单
	 *
	 * @param request
	 * @return
	 */
	@PostMapping("/createOrder")
	@LogDetail
	public CommonResult<CreateOrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
		return createOrderService.createOrder(request);
	}

	/**
	 * 顾客用券
	 *
	 * @param request
	 * @return
	 */
	@PostMapping("/useCoupon")
	@LogDetail
	public CommonResult<UseCouponResponse> useCoupon(@RequestBody UseCouponRequest request) {
		return useCouponService.useCoupon(request);
	}

	/**
	 * 订单支付中
	 *
	 * @param request
	 * @return
	 */
	@LogDetail
	@PostMapping("/payingOrder")
	public CommonResult<PayingOrderResponse> payingOrder(@RequestBody PayingOrderRequest request) {
		return payingOrderService.payingOrder(request);
	}

	/**
	 * 订单支付完成
	 *
	 * @param request
	 * @return
	 */
	@LogDetail
	@PostMapping("/orderSuccess")
	public CommonResult<Void> orderSuccess(@RequestBody OrderSuccessRequest request) {
		return orderSuccessService.orderSuccess(request);
	}

	/**
	 * 订单发货
	 *
	 * @param request
	 * @return
	 */
	@PostMapping("/shipping")
	public CommonResult<Void> shipping(@RequestBody ShippingRequest request) {
		return shippingOrderService.orderShipping(request);
	}

	/**
	 * 确认收货
	 *
	 * @param request
	 * @return
	 */
	@PostMapping("/receive")
	public CommonResult<Void> receive(@RequestBody ReceiveOrderRequest request) {
		return receiveOrderService.orderReceive(request);
	}

	/**
	 * 根据用户id查找用户
	 *
	 * @param userId
	 * @return
	 */
	@GetMapping("/findUser")
	public CommonResult<UserQueryResponse> findUser(@RequestParam("userId") String userId) {
		return feign.findUserByUserIdOrLoginName(new UserQueryRequest().setUserId(userId));
	}

	/**
	 * 查询用户的订单
	 *
	 * @param request
	 * @return
	 */
	@PostMapping("/findOrder")
	public CommonResult<List<OrderQueryResponse>> findOrder(@RequestBody OrderQueryRequest request) {
		return orderQueryService.findOrder(request);
	}

}
