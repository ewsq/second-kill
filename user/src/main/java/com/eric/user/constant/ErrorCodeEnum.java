package com.eric.user.constant;

/**
 * 返回给前端的错误码
 *
 * @author wang.js on 2019/1/23.
 * @version 1.0
 */
public enum ErrorCodeEnum {

	LOGIN_SUCCESS("0", "登陆成功"),
	UNKNOW_OPERATION_TYPE("250", "未知操作类型"),
	ERROR_PASSWORD("251", "密码不正确"),
	USER_REGISTER_FAIL("252", "用户注册失败"),
	LOGIN_NAME_NOT_REGISTER("253", "用户名未注册"),
	ACCOUNT_FREEZE("254", "账户已冻结"),
	ACCOUNT_DISACTIVE("255", "账户已失效"),
	UPDATE_SUCCESS("0", "更新成功"),
	UPDATE_FAIL("256", "更新失败"),
	ERROR_SAVE("262", "保存失败"),
	EMPTY_USER_ID("257", "用户id不能为空"),
	USER_NOT_FOUND("258", "用户不存在"),
	UPDATE_FORBIDDEN("259", "当前状态禁止修改"),
	DUPLICATE("261", "重复单号"),
	BALANCE_NOT_ENOUGH("263", "余额不足"),
	ERROR_SIGN("260", "签名不合法"),
	USER_ABNORMAL("261", "用户状态不正常"),
	RESET_PASSWORD_SUCCESS("0", "密码重置成功"),
	RESET_PASSWORD_FAIL("262", "密码重置失败"),
	POINT_NOT_ENOUGH("263", "用户积分不足"),
	;

	/**
	 * 错误码
	 */
	private String errorCode;
	/**
	 * 错误信息
	 */
	private String errorMsg;


	ErrorCodeEnum(String errorCode, String errorMsg) {
		this.errorCode = errorCode;
		this.errorMsg = errorMsg;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}
}
