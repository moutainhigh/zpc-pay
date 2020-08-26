package com.aomi.pay.exception;

import com.aomi.pay.domain.CommonErrorCode;

/**
 * @Author hdq
 * @Date 2020/7/15 14:40
 * @Description 业务级别异常
 */
public class BusinessException extends AbstractGenericException {

    private static final long serialVersionUID = 1L;

    /**
     * <br>------------------------------<br>
     *
     * @param desc
     */
    public BusinessException(String desc) {
        super(desc);
    }

    /**
     * <br>------------------------------<br>
     *
     * @param code 返回码
     * @param desc 返回描述
     */
    public BusinessException(String code, String desc) {
        super(code, desc);
    }

    /**
     * <br>------------------------------<br>
     *
     * @param code 返回码
     * @param desc 返回描述
     * @param throwable
     */
    public BusinessException(String code, String desc, Throwable throwable) {
        super(code, desc, throwable);
    }

    /**
     * <br>------------------------------<br>
     *
     * @param code 返回码
     * @param throwable
     */
    public BusinessException(String code, Throwable throwable) {
        super(code, throwable);
    }

    /**
     * <br>------------------------------<br>
     *
     * @param code 返回码
     * @param arguments
     */
    public BusinessException(String code, Object... arguments) {
        super(code, arguments);
    }

    /**
     * <br>------------------------------<br>
     *
     * @param commonErrorCode 公共异常编码
     */
    public BusinessException(CommonErrorCode commonErrorCode) {
        super(commonErrorCode.getCode(), commonErrorCode.getDesc());
    }

}
