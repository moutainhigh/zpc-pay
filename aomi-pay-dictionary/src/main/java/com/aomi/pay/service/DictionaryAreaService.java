package com.aomi.pay.service;

import com.aomi.pay.vo.DictionaryAreaVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 银行编码数据字典Service
 *
 * @author : hdq
 * @date 2020/8/11
 */
@Transactional(rollbackFor = Exception.class)
public interface DictionaryAreaService {

    /**
     * 获取所有银行编码信息
     */
    List<DictionaryAreaVO> getAllArea();

}
