package com.aomi.pay.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * @Author hdq
 * @Date 2020/8/1 17:23
 * @Version 1.0
 * @Desc  商户基本信息
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class MchtBase {


    /**
     * 机构方商户号
     */
    private Long instMchtNo;
    private String mchtName;
    private String mchtNo;
    private String simpleName;
    private String mchtKind;
    private String areaNo;
    private String address;
    private String storePhone;
    private String mchtScope;
    private String mchtType;
    private String status;
    private String nuionpayMacht;

}
