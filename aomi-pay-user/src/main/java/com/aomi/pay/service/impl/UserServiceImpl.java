package com.aomi.pay.service.impl;

import com.alibaba.fastjson.JSON;
import com.aomi.pay.domain.CommonErrorCode;
import com.aomi.pay.entity.*;
import com.aomi.pay.exception.BusinessException;
import com.aomi.pay.feign.ApiClient;
import com.aomi.pay.mapper.*;
import com.aomi.pay.service.UserService;
import com.aomi.pay.util.*;
import com.aomi.pay.vo.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Base64Utils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import sun.misc.BASE64Decoder;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RefreshScope
public class UserServiceImpl implements UserService {


    @Autowired
    private MerchantProductMapper merchantProductMapper;
    @Autowired
    private MerchantProductBindMapper merchantProductBindMapper;
    @Autowired
    private MerchantInfoMapper merchantInfoMapper;
    @Autowired
    private MerchantImgMapper merchantImgMapper;
    @Autowired
    private ApiClient apiClient;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private MerchantQrBindMapper merchantQrBindMapper;



    /**
     * 机构插入商户信息
     */
    @Override
    public MerchantInfoVO insertMerchantInfo(MerchantInfoVO merchantInfoVO) throws Exception {
        if(StringUtils.isEmpty(String.valueOf(merchantInfoVO))){
            throw new BusinessException(CommonErrorCode.E_301009);//填写信息为空
        }
        Long instMchtNo = merchantInfoVO.getMchtBase().getInstMchtNo();
        MerchantInfo beforeMerchantInfo = merchantInfoMapper.selectById(instMchtNo);
        MerchantInfo merchantInfo = null;
        if (beforeMerchantInfo != null){
            merchantInfo = new MerchantInfo();
            MchtBase mchtBase = merchantInfoVO.getMchtBase();
            merchantInfo.setMerchantName(mchtBase.getMchtName());
            merchantInfo.setSimpleName(mchtBase.getSimpleName());
            merchantInfo.setMerchantKind(mchtBase.getMchtKind());
            merchantInfo.setAreaNo(mchtBase.getAreaNo());
            merchantInfo.setAddress(mchtBase.getAddress());
            merchantInfo.setMerchantPhone(mchtBase.getStorePhone());
            merchantInfo.setMerchantScope(mchtBase.getMchtScope());
            merchantInfo.setMerchantType(mchtBase.getMchtType());
            merchantInfo.setUnionpayMerchant(mchtBase.getNuionpayMacht());
            merchantInfoMapper.update(merchantInfo,new LambdaQueryWrapper<MerchantInfo>().eq(MerchantInfo::getId,instMchtNo));
        }else {
            merchantInfo = new MerchantInfo();
            MchtBase mchtBase = merchantInfoVO.getMchtBase();
            merchantInfo.setId(instMchtNo);
            merchantInfo.setBdNo(mchtBase.getBdNo());
            merchantInfo.setMerchantName(mchtBase.getMchtName());
            merchantInfo.setSimpleName(mchtBase.getSimpleName());
            merchantInfo.setMerchantKind(mchtBase.getMchtKind());
            merchantInfo.setAreaNo(mchtBase.getAreaNo());
            merchantInfo.setAddress(mchtBase.getAddress());
            merchantInfo.setMerchantPhone(mchtBase.getStorePhone());
            merchantInfo.setMerchantScope(mchtBase.getMchtScope());
            merchantInfo.setMerchantType(mchtBase.getMchtType());
            merchantInfo.setUnionpayMerchant(mchtBase.getNuionpayMacht());
            merchantInfoMapper.insert(merchantInfo);
        }

        MchtUser mchtUser = merchantInfoVO.getMchtUser();
        merchantInfo.setLegalPersonEmail(mchtUser.getEmail());
        merchantInfo.setLegalPersonName(mchtUser.getName());
        merchantInfo.setLegalPersonPhone(mchtUser.getPhone());
        if (StringUtils.isNotEmpty(mchtUser.getCardNo())){
            merchantInfo.setLegalPersonCardno(AesUtil.encrypt(mchtUser.getCardNo()));//加密入库
        }
        merchantInfo.setLegalPersonCardnoDate(mchtUser.getCardDate());
        merchantInfoMapper.updateById(merchantInfo);

        MchtAcct mchtAcct = merchantInfoVO.getMchtAcct();
        merchantInfo.setAcctProxy(mchtAcct.getAcctProxy());
        merchantInfo.setAgentCardNo(mchtAcct.getAgentCardNo());
        merchantInfo.setAgentCardDate(mchtAcct.getAgentCardDate());
        merchantInfo.setAcctType(mchtAcct.getAcctType());
        if (StringUtils.isNotEmpty(mchtAcct.getAcctNo())){
            merchantInfo.setAcctNo(AesUtil.encrypt(mchtAcct.getAcctNo()));//加密入库
        }
        if(StringUtils.isNotEmpty(mchtAcct.getAcctName())){
            merchantInfo.setAcctName(AesUtil.encrypt(mchtAcct.getAcctName()));
        }
        merchantInfo.setAcctBankNo(mchtAcct.getAcctBankNo());
        merchantInfo.setAcctZbankCode(mchtAcct.getAcctZbankCode());
        merchantInfo.setAcctZbankNo(mchtAcct.getAcctZbankNo());
        merchantInfoMapper.updateById(merchantInfo);

        MchtComp mchtComp = merchantInfoVO.getMchtComp();
        merchantInfo.setLicenseDate(mchtComp.getLicenseDate());
        if (StringUtils.isNotEmpty(mchtComp.getLicenseNo())){
            merchantInfo.setLicenseNo(AesUtil.encrypt(mchtComp.getLicenseNo()));
        }
        merchantInfo.setLicenseType(mchtComp.getLicenseType());
        merchantInfoMapper.updateById(merchantInfo);

        merchantInfo.setSn(merchantInfoVO.getSn());
        merchantInfo.setSnModelId(merchantInfoVO.getSnModelId());
        merchantInfo.setServiceType(merchantInfoVO.getServiceType());
        merchantInfo.setCreateTime(LocalDateTime.now());
        merchantInfo.setStatus("1");
        merchantInfoMapper.updateById(merchantInfo);

        MerchantInfo afterMerchantInfo = merchantInfoMapper.selectById(instMchtNo);
        System.out.println(afterMerchantInfo);
        return null;
    }



    /**
     * 机构查询商户信息
     */
    @Override
    public MerchantInfoVO findMerchantInfo(JSONObject str) {
        String instMchtNo = str.getString("instMchtNo");
        if(StringUtils.isEmpty(String.valueOf(instMchtNo))){
            throw new BusinessException(CommonErrorCode.E_301003);//填写信息为空
        }
        MerchantInfo rmerchantInfo = merchantInfoMapper.selectOne(new LambdaQueryWrapper<MerchantInfo>().eq(MerchantInfo::getId,instMchtNo));
        MerchantInfoVO merchantInfoVO = new MerchantInfoVO();
        if (StringUtils.isNotEmpty(String.valueOf(rmerchantInfo))){
            merchantInfoVO = this.common(rmerchantInfo);
        }
        return merchantInfoVO;
    }



    /**
     * 商户信息入网 入平台 拆开后
     */
    @Override
    public String create(JSONObject str) throws Exception {

        Long instMchtNo = str.getLong("instMchtNo");
        if (instMchtNo == null){
            CommonExceptionUtils.throwParamException(CommonErrorCode.E_301003);
        }
        MerchantInfo merchantInfo = merchantInfoMapper.selectById(instMchtNo);

        MchtMedia mchtMedia = getString(instMchtNo);
        MerchantInfoVO merchantInfoVO = this.common(merchantInfo);
        merchantInfoVO.setMchtMedia(mchtMedia);

        //todo 异常处理
        Object data = apiClient.createOrgMcht(merchantInfoVO).getData();
        if (ObjectUtils.equals(data, null)) {
            CommonExceptionUtils.throwBusinessException(CommonErrorCode.E_301010);
        }
        JSONObject jsonObject = JSONObject.fromObject(data);

        String desc = jsonObject.getString("desc");
        //todo 处理返回值
        if (StringUtils.isNotEmpty(desc)){
            return desc;
        }

        String mchtNo = jsonObject.getString("mchtNo");
        merchantInfo.setPlatformId(mchtNo);
        String unionPayMchtNo = jsonObject.getString("unionPayMchtNo");
        if (StringUtils.isNotEmpty(unionPayMchtNo)) {
            merchantInfo.setUnionPayMchtNo(unionPayMchtNo);
        }
        //存入redis
        redisTemplate.opsForValue().set(merchantInfo.getId().toString(), mchtNo);
        merchantInfoMapper.updateById(merchantInfo);

        return null;
    }



    /**
     * 商户开通产品
     */
    @Override
    public void openMcht(ProductVO productVO) throws Exception {
        if (productVO.getInstMchtNo() == null || productVO.getProductId() == null) {
            throw new BusinessException(CommonErrorCode.E_301009);//填写信息为空
        }
        String platformId = this.merchantInfoMapper.selectById(productVO.getInstMchtNo()).getPlatformId();
        Integer productId = productVO.getProductId();//产品id
        MerchantProduct merchantProduct = merchantProductMapper.selectById(productId);
        productVO.setMchtNo(platformId);
        productVO.setModelId(merchantProduct.getModelId());
        productVO.setProductCode(merchantProduct.getProductCode());

        Object data = apiClient.addProduct(productVO).getData();
        if (ObjectUtils.equals(data, null)) {
            throw new BusinessException(CommonErrorCode.E_301010);
        }
        MerchantProductBind merchantProductBind = new MerchantProductBind();
        //插入绑定关系表
        merchantProductBind.setMerchantId(productVO.getInstMchtNo());//商户机构id
        merchantProductBind.setProductId(productId);
        merchantProductBind.setState(1);
        merchantProductBind.setCreateTime(LocalDateTime.now());
        merchantProductBindMapper.insert(merchantProductBind);
    }

    @Override
    public JSONObject queryByfixedQrCode(JSONObject str) {
        String fixedQrCode = str.getString("fixedQrCode");
        if (StringUtils.isEmpty(fixedQrCode)){
            throw new BusinessException(CommonErrorCode.E_301012);
        }
        MerchantQrBind merchantQrBind = this.merchantQrBindMapper.selectOne(new LambdaQueryWrapper<MerchantQrBind>().eq(MerchantQrBind::getFixedQrCode, fixedQrCode));
        MerchantInfo merchantInfo = this.merchantInfoMapper.selectById(merchantQrBind.getMerchantId());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("instMchtNo",merchantInfo.getId());
        jsonObject.put("simpleName",merchantInfo.getSimpleName());
        return jsonObject;
    }

    @Override
    public JSONObject queryMchtInfoByPhoto(JSONObject str) {
        String fixedQrCode = str.getString("fixedQrCode");
        if (StringUtils.isEmpty(fixedQrCode)){
            throw new BusinessException(CommonErrorCode.E_301012);
        }
        MerchantQrBind merchantQrBind = this.merchantQrBindMapper.selectOne(new LambdaQueryWrapper<MerchantQrBind>().eq(MerchantQrBind::getFixedQrCode, fixedQrCode));
        MerchantInfo merchantInfo = this.merchantInfoMapper.selectById(merchantQrBind.getMerchantId());
        JSONObject jsonObject = new JSONObject();
        Long instMchtNo = merchantInfo.getId();
        jsonObject.put("instMchtNo",instMchtNo);
        jsonObject.put("mchtNo",merchantInfo.getPlatformId());
        jsonObject.put("simpleName",merchantInfo.getSimpleName());
        jsonObject.put("bdNo",merchantInfo.getBdNo());

        List<MerchantProductBind> merchantProductBinds = this.merchantProductBindMapper.selectList(new LambdaQueryWrapper<MerchantProductBind>().eq(MerchantProductBind::getMerchantId, instMchtNo));
        List<MerchantProduct> collect = merchantProductBinds.stream().map(merchantProductBind -> {
            Integer productId = merchantProductBind.getProductId();
            MerchantProduct merchantProduct = this.merchantProductMapper.selectById(productId);
            return merchantProduct;
        }).collect(Collectors.toList());
        jsonObject.put("product",collect);
        redisTemplate.opsForValue().set(fixedQrCode, String.valueOf(jsonObject));
        return jsonObject;
    }


    /**前端传来base64字符串
     * 商户上传图片
     */
    @Override
    public String uploadImg(String imageStr,String picType,String userId) throws Exception {
        log.info("--------商户上传图片--------");
        MerchantImg merchantImg = new MerchantImg();

       Integer imageSize =  imageSize(imageStr);
        if (imageSize > 2097152){
            throw new BusinessException(CommonErrorCode.E_301005);//暂定
        }
        PictureVO pictureVO = new PictureVO();
        pictureVO.setImagStr(imageStr);
        pictureVO.setPicType(picType);

        //调用环迅图片上传接口
        Object data = apiClient.uploadImg(pictureVO).getData();
        JSONObject jsonObject = JSONObject.fromObject(data);
        String imgCode = jsonObject.getString("imgCode");

        //图片上传到阿里云
        String imgOss = this.uploadImgStringOss(imageStr,userId);

        merchantImg.setImgUrl(imgOss);
        merchantImg.setImgCode(imgCode);
        merchantImg.setType(picType);
        merchantImg.setUserId(userId);
        merchantImg.setUpdateTime(LocalDateTime.now());
        merchantImg.setPlatformTag("hx");

        //插入数据库
        merchantImgMapper.insert(merchantImg);
        return null;
    }


    public MultipartFile base64toMultipart(String data) {
        try {
            String[] baseStrs = data.split(",");
            BASE64Decoder decoder = new BASE64Decoder();
            byte[] b = decoder.decodeBuffer(baseStrs[1]);
            for(int i = 0; i < b.length; ++i) {
                if (b[i] < 0) {
                    b[i] += 256;
                }
            }
            return new MultipartFileUtil(b, baseStrs[0]);
        } catch (IOException e) {
            throw new RuntimeException("IO流异常" , e);
        }
    }


//
//    /**
//     * 商户上传图片
//     */
//    @Override
//    public String uploadImg(HttpServletRequest request,String picType,String userId) throws Exception {
//        log.info("--------商户上传图片--------");
//        MerchantImg merchantImg = new MerchantImg();
//
//        //图片转化为字节
//        String photo = null;
//        try {
//            photo = getPhoto(request);
//        } catch (BusinessException businessException) {
//            throw new BusinessException(CommonErrorCode.E_301005);
//        }
//
//        PictureVO pictureVO = new PictureVO();
//        pictureVO.setImagStr(photo);
//        pictureVO.setPicType(picType);
//
//        //调用环迅图片上传接口
//        Object data = apiClient.uploadImg(pictureVO).getData();
//        JSONObject jsonObject = JSONObject.fromObject(data);
//        String imgCode = jsonObject.getString("imgCode");
//
//        //图片上传到阿里云
////        String imgOss = this.uploadImgOss(request,userId);
//        String imgOss = this.uploadImgStringOss(photo,userId);
//        merchantImg.setImgUrl(imgOss);
//        merchantImg.setImgCode(imgCode);
//        merchantImg.setType(picType);
//        merchantImg.setUserId(userId);
//        merchantImg.setUpdateTime(LocalDateTime.now());
//        merchantImg.setPlatformTag("hx");
//
//        //插入数据库
//        merchantImgMapper.insert(merchantImg);
//        return null;
//    }

//
//    /**
//     * base64商户上传图片
//     */
//    public String stringUploadImg(String imageStr,String picType,String userId) throws Exception {
//        log.info("--------商户上传图片--------");
//        MerchantImg merchantImg = new MerchantImg();
//
//        //图片转化为字节
//        String photo = null;
//        try {
//            photo = getPhoto(request);
//        } catch (BusinessException businessException) {
//            throw new BusinessException(CommonErrorCode.E_301005);
//        }
//
//        PictureVO pictureVO = new PictureVO();
//        pictureVO.setImagStr(photo);
//        pictureVO.setPicType(picType);
//
//        //调用环迅图片上传接口
//        Object data = apiClient.uploadImg(pictureVO).getData();
//        JSONObject jsonObject = JSONObject.fromObject(data);
//        String imgCode = jsonObject.getString("imgCode");
//
//        //图片上传到阿里云
//        String imgOss = this.uploadImgOss(request,userId);
//
//        merchantImg.setImgUrl(imgOss);
//        merchantImg.setImgCode(imgCode);
//        merchantImg.setType(picType);
//        merchantImg.setUserId(userId);
//        merchantImg.setUpdateTime(LocalDateTime.now());
//        merchantImg.setPlatformTag("hx");
//
//        //插入数据库
//        merchantImgMapper.insert(merchantImg);
//        return null;
//    }







    /**
     * 商户入网信息查询
     *
     * @return
     */
    @Override
    public MerchantInfoVO queryMcht(JSONObject str) throws Exception {
        Long instMchtNo = str.getLong("instMchtNo");
        String mchtNo = str.getString("mchtNo");
        if (StringUtils.isEmpty(mchtNo) && instMchtNo == null) {
            throw new BusinessException(CommonErrorCode.E_301007);//暂定
        }
        //
        BaseResponse baseResponse = apiClient.queryMcht(str);
        Object data = baseResponse.getData();
        if (ObjectUtils.equals(data, null)) {
            throw new BusinessException(CommonErrorCode.E_301010);
        }
        String jsonString = JSON.toJSONString(data);
        MerchantInfoVO merchantInfoVO = JSON.parseObject(jsonString, MerchantInfoVO.class);
        return merchantInfoVO;
    }


    /**
     * 查询商户审核状态
     */
    @Override
    public JSONObject queryMchtAudit(JSONObject str) throws Exception {
        String mchtNo = str.getString("mchtNo");
        if (StringUtils.isEmpty(mchtNo)){
            throw new BusinessException(CommonErrorCode.E_301003);//暂定
        }
        log.info("--------查询商户审核状态--------");
        Object data = apiClient.queryMchtAudit(str).getData();
        if (ObjectUtils.equals(data, null)) {
            throw new BusinessException(CommonErrorCode.E_301010);
        }
        JSONObject jsonObject = JSONObject.fromObject(data);
        return jsonObject;
    }


    /**
     * 修改商户入网信息
     *
     */
    @Override
    public void updateInfo(MerchantInfoVO merchantInfoVO) throws Exception {
        if (StringUtils.isEmpty(String.valueOf(merchantInfoVO))){
            throw new BusinessException(CommonErrorCode.E_301009);
        }
        Long instMchtNo = merchantInfoVO.getMchtBase().getInstMchtNo();//商户机构id

        MerchantInfo merchantInfo = new MerchantInfo();

        MchtBase mchtBase = merchantInfoVO.getMchtBase();
        merchantInfo.setMerchantName(mchtBase.getMchtName());
        merchantInfo.setSimpleName(mchtBase.getSimpleName());
        merchantInfo.setMerchantKind(mchtBase.getMchtKind());
        merchantInfo.setAreaNo(mchtBase.getAreaNo());
        merchantInfo.setAddress(mchtBase.getAddress());
        merchantInfo.setMerchantPhone(mchtBase.getStorePhone());
        merchantInfo.setMerchantScope(mchtBase.getMchtScope());
        merchantInfo.setMerchantType(mchtBase.getMchtType());
        merchantInfo.setUnionpayMerchant(mchtBase.getNuionpayMacht());

        MchtUser mchtUser = merchantInfoVO.getMchtUser();
        merchantInfo.setLegalPersonEmail(mchtUser.getEmail());
        merchantInfo.setLegalPersonName(mchtUser.getName());
        merchantInfo.setLegalPersonPhone(mchtUser.getPhone());
        merchantInfo.setLegalPersonCardno(mchtUser.getCardNo());
        merchantInfo.setLegalPersonCardnoDate(mchtUser.getCardDate());

        MchtAcct mchtAcct = merchantInfoVO.getMchtAcct();
        merchantInfo.setAcctProxy(mchtAcct.getAcctProxy());
        merchantInfo.setAgentCardNo(mchtAcct.getAgentCardNo());
        merchantInfo.setAgentCardDate(mchtAcct.getAgentCardDate());
        merchantInfo.setAcctType(mchtAcct.getAcctType());
        merchantInfo.setAcctNo(mchtAcct.getAcctNo());
        merchantInfo.setAcctName(mchtAcct.getAcctName());
        merchantInfo.setAcctBankNo(mchtAcct.getAcctBankNo());
        merchantInfo.setAcctZbankCode(mchtAcct.getAcctZbankCode());
        merchantInfo.setAcctZbankNo(mchtAcct.getAcctZbankNo());

        MchtComp mchtComp = merchantInfoVO.getMchtComp();
        merchantInfo.setLicenseDate(mchtComp.getLicenseDate());
        merchantInfo.setLicenseNo(mchtComp.getLicenseNo());
        merchantInfo.setLicenseType(mchtComp.getLicenseType());


        merchantInfo.setSn(merchantInfoVO.getSn());
        merchantInfo.setSnModelId(merchantInfoVO.getSnModelId());
        merchantInfo.setServiceType(merchantInfoVO.getServiceType());
        merchantInfo.setStatus("0");


        log.info("--------修改商户入网信息--------");
        Object data = apiClient.updateMchtInfo(merchantInfoVO).getData();
        if (ObjectUtils.equals(data, null)) {
            throw new BusinessException(CommonErrorCode.E_301010);
        }
//        JSONObject jsonObject = JSONObject.fromObject(data);
        merchantInfoMapper.update(merchantInfo,new LambdaQueryWrapper<MerchantInfo>().eq(MerchantInfo::getId,instMchtNo));
    }

    /**
     * 修改商户状态
     */
    @Override
    public void updateStatus(JSONObject str) throws Exception {
        log.info("--------修改商户状态--------");
//        String mchtNo = str.getString("mchtNo");
//        String unionPayMchtNo = str.getString("unionPayMchtNo");
        //todo 待修改
        Object data = apiClient.updateMchtStatus(str).getData();
        if (ObjectUtils.equals(data, null)) {
            throw new BusinessException(CommonErrorCode.E_301010);
        }
    }

    /**
     * 修改商户开通产品签约费率
     */
    @Override
    public void updateProductModel(JSONObject str) throws Exception {
        Long instMchtNo = str.getLong("instMchtNo");
        if (instMchtNo == null){
            throw new BusinessException(CommonErrorCode.E_301003);//暂定
        }
        Integer productId = str.getInt("productId");
        if (productId == null){
            throw new BusinessException(CommonErrorCode.E_301011);//暂定
        }

        MerchantInfo merchantInfo = merchantInfoMapper.selectOne(new LambdaQueryWrapper<MerchantInfo>().eq(MerchantInfo::getId, instMchtNo));
        MerchantProduct merchantProduct = merchantProductMapper.selectById(productId);
        str.put("mchtNo",merchantInfo.getPlatformId());
        str.put("modelId",merchantProduct.getModelId());
        str.put("productCode",merchantProduct.getProductCode());

        //todo 待修改
        log.info("--------修改商户开通产品签约费率--------");
        Object data = apiClient.updateProductModel(str).getData();
        if (ObjectUtils.equals(data, null)) {
            throw new BusinessException(CommonErrorCode.E_301010);
        }

    }

    /**
     *  修改商户结算银行卡信息
     */
    @Override
    public void updateMchtAcct(AcctVO acctVO) throws Exception {
        //todo 待修改
        log.info("--------修改商户结算银行卡信息--------");
        Object data = apiClient.updateMchtAcct(acctVO).getData();
        if (ObjectUtils.equals(data, null)) {
            throw new BusinessException(CommonErrorCode.E_301010);
        }
    }




    //获取imageCode
    private MchtMedia getString(Long instMchtNo) {

        List<MerchantImg> merchantImgs = merchantImgMapper.selectList(new LambdaQueryWrapper<MerchantImg>().eq(MerchantImg::getUserId, instMchtNo));
        MchtMedia mchtMedia = new MchtMedia();
        merchantImgs.forEach(merchantImg -> {
            String type = merchantImg.getType();
            String imgCode = merchantImg.getImgCode();
            if (StringUtils.equals(type,"01")){
                mchtMedia.setCertFront(imgCode);
            }
            if (StringUtils.equals(type,"02")){
                mchtMedia.setCertReverse(imgCode);
            }
            if (StringUtils.equals(type,"03")){
                mchtMedia.setHandheld(imgCode);
            }
            if (StringUtils.equals(type,"04")){
                mchtMedia.setBankCardPositive(imgCode);
            }
            if (StringUtils.equals(type,"05")){
                mchtMedia.setLicense(imgCode);
            }
            if (StringUtils.equals(type,"06")){
                mchtMedia.setOrgImage(imgCode);
            }
            if (StringUtils.equals(type,"07")){
                mchtMedia.setTaxImage(imgCode);
            }
            if (StringUtils.equals(type,"08")){
                mchtMedia.setOpenAccoLic(imgCode);
            }
            if (StringUtils.equals(type,"09")){
                mchtMedia.setDoorHead(imgCode);
            }
            if (StringUtils.equals(type,"10")){
                mchtMedia.setCashier(imgCode);
            }
            if (StringUtils.equals(type,"11")){
                mchtMedia.setShopPanoram(imgCode);
            }
            if (StringUtils.equals(type,"12")){
                mchtMedia.setPriLicAgree(imgCode);
            }
            if (StringUtils.equals(type,"13")){
                mchtMedia.setAgenCardFront(imgCode);
            }
            if (StringUtils.equals(type,"14")){
                mchtMedia.setAgenIdCardBackPic(imgCode);
            }
            if (StringUtils.equals(type,"15")){
                mchtMedia.setAgentCardId(imgCode);
            }
            if (StringUtils.equals(type,"16")){
                mchtMedia.setAgentProtocol(imgCode);
            }
        });
        return mchtMedia;
    }


    // 数据组装
    public MerchantInfoVO common(MerchantInfo merchantInfo){

        MerchantInfoVO merchantInfoVO = new MerchantInfoVO();

        merchantInfoVO.setSn(merchantInfo.getSn());
        merchantInfoVO.setSnModelId(merchantInfo.getSnModelId());
        merchantInfoVO.setServiceType(merchantInfo.getServiceType());
        merchantInfoVO.setChannelKind(merchantInfo.getChannelKind());

        MchtAcct mchtAcct = new MchtAcct();
        mchtAcct.setAcctBankNo(merchantInfo.getAcctBankNo());
        mchtAcct.setAcctName(merchantInfo.getAcctName());
        mchtAcct.setAcctNo(merchantInfo.getAcctNo());
        mchtAcct.setAcctProxy(merchantInfo.getAcctProxy());
        mchtAcct.setAcctType(merchantInfo.getAcctType());
        mchtAcct.setAcctZbankCode(merchantInfo.getAcctZbankCode());
        mchtAcct.setAcctZbankNo(merchantInfo.getAcctZbankNo());
        mchtAcct.setAgentCardDate(merchantInfo.getAgentCardDate());
        mchtAcct.setAgentCardNo(merchantInfo.getAgentCardNo());

        merchantInfoVO.setMchtAcct(mchtAcct);

        MchtBase mchtBase = new MchtBase();
        mchtBase.setAddress(merchantInfo.getAddress());
        mchtBase.setAreaNo(merchantInfo.getAreaNo());
        mchtBase.setMchtKind(merchantInfo.getMerchantKind());
        mchtBase.setMchtName(merchantInfo.getAcctName());
        mchtBase.setMchtScope(merchantInfo.getMerchantScope());
        mchtBase.setMchtType(merchantInfo.getAcctType());
        mchtBase.setNuionpayMacht(merchantInfo.getUnionpayMerchant());
        mchtBase.setSimpleName(merchantInfo.getSimpleName());
        mchtBase.setStorePhone(merchantInfo.getMerchantPhone());

        merchantInfoVO.setMchtBase(mchtBase);

        MchtComp mchtComp = new MchtComp();
        mchtComp.setLicenseDate(merchantInfo.getLicenseDate());
        mchtComp.setLicenseNo(merchantInfo.getLicenseNo());
        mchtComp.setLicenseType(merchantInfo.getLicenseType());
        merchantInfoVO.setMchtComp(mchtComp);

        MchtUser mchtUser = new MchtUser();
        mchtUser.setCardDate(merchantInfo.getLegalPersonCardnoDate());
        mchtUser.setCardNo(merchantInfo.getLegalPersonCardno());
        mchtUser.setEmail(merchantInfo.getLegalPersonEmail());
        mchtUser.setName(merchantInfo.getLegalPersonName());
        mchtUser.setPhone(merchantInfo.getLegalPersonPhone());
        merchantInfoVO.setMchtUser(mchtUser);

        return merchantInfoVO;
    }



    /**
     * 上传图片文件返回图片byte[]
     */
//    @PostMapping("user/change/photoByte")
    public String getPhoto(HttpServletRequest request) throws IOException {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        MultipartFile image = multipartRequest.getFile("image");
        long imageSize = image.getSize();
        System.out.println(imageSize);
        if (imageSize > 2097152){
            throw new BusinessException(CommonErrorCode.E_301005);//暂定
        }

        InputStream inStream = image.getInputStream();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        //创建一个Buffer字符串
        byte[] buffer = new byte[1024];
        //每次读取的字符串长度，如果为-1，代表全部读取完毕
        int len = 0;
        //使用一个输入流从buffer里把数据读取出来
        while ((len = inStream.read(buffer)) != -1) {
            //用输出流往buffer里写入数据，中间参数代表从哪个位置开始读，len代表读取的长度
            outStream.write(buffer, 0, len);
        }
        inStream.close();
        String imageStr = Base64Utils.encodeToString(buffer);
        return imageStr;
    }



    public Integer imageSize(String imageStr) {
//        String str = image.substring(22); // 1.需要计算文件流大小，首先把头部的data:image/png;base64,（注意有逗号）去掉。
        Integer equalIndex = imageStr.indexOf("=");//2.找到等号，把等号也去掉
        if (imageStr.indexOf("=") > 0) {
            imageStr = imageStr.substring(0, equalIndex);
        }
        Integer strLength = imageStr.length();//3.原来的字符流大小，单位为字节
        Integer size = strLength - (strLength / 8) * 2;//4.计算后得到的文件流大小，单位为字节
        return size;
    }






        /**
         * 上传图片到阿里云
         * 添加商品图片(上传图片)
         */
    public String uploadImgOss(HttpServletRequest request,String userId) {
        String imgurl = "";
        try {
            MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
            MultipartFile file = multipartRequest.getFile("image");

            if (file == null) {
                throw new BusinessException(CommonErrorCode.FAIL.getCode(), "请上传至少一张图片");
            }
            String ossObjectNamePrefix = AliOSSUtil.APP_SYS_IMAGETEXT + "-";
            String ossObjectName = "";
            if (file != null) {
                String fileName = file.getOriginalFilename();
                String prefix = fileName.substring(fileName.lastIndexOf("."));
                fileName = System.currentTimeMillis() + prefix; //字符串+商户号+时间戳
                String StringUserId = userId + "-" + IdWorker.getTimeId().substring(0,10) + "-";
                ossObjectName = StringUserId + ossObjectNamePrefix + fileName;
                AliOSSUtil aliOSSUtil = new AliOSSUtil();

                try {
                    aliOSSUtil.uploadStreamToOss(ossObjectName, file.getInputStream());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                String fileUrl = aliOSSUtil.getFileUrl(ossObjectName);
                imgurl = imgurl + fileUrl.substring(0, fileUrl.lastIndexOf("?"));
            }
        } catch (BusinessException businessException) {
            throw new BusinessException(CommonErrorCode.E_301001);//暂定
        }
        return imgurl;
    }



    /**
     * 上传图片到阿里云
     * 添加商品图片(上传图片)
     */
    public String uploadImgStringOss(String imageStr,String userId) throws IOException {
        String imgurl = "";
        try {
            MultipartFile file = base64toMultipart(imageStr);
            if (file == null) {
                throw new BusinessException(CommonErrorCode.FAIL.getCode(), "请上传至少一张图片");
            }
//            String ossObjectNamePrefix = AliOSSUtil.APP_SYS_IMAGETEXT + "-";
            String ossObjectName = "";
            if (file != null) {
//                String fileName = file.getOriginalFilename();
                String contentType = file.getContentType();
//                String prefix = fileName.substring(fileName.lastIndexOf("."));
//                fileName = System.currentTimeMillis() + prefix; //字符串+商户号+时间戳
                long l = System.currentTimeMillis();
                String StringUserId = "jubao-" + userId + "-" + IdWorker.getTimeId().substring(0,10) + ".";
                ossObjectName = StringUserId + contentType;
                AliOSSUtil aliOSSUtil = new AliOSSUtil();
                try {
                    aliOSSUtil.uploadStreamToOss(ossObjectName, file.getInputStream());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                String fileUrl = aliOSSUtil.getFileUrl(ossObjectName);
                imgurl = imgurl + fileUrl.substring(0, fileUrl.lastIndexOf("?"));
            }
            String filename = file.getOriginalFilename();
        } catch (BusinessException businessException) {
            throw new BusinessException(CommonErrorCode.E_301001);//暂定
        }
        return imgurl;
    }
//
//    /**
//     *  根据字节流获取图片类型
//     * */
//    public static String getImageType(String s) {
//        try {
//            byte[] decode = Base64Utils.decodeFromString(s);
////            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageBytes);
////            MemoryCacheImageInputStream ImageInputStream = new MemoryCacheImageInputStream(byteArrayInputStream);
//            InputStream  InputStream = new ByteArrayInputStream(decode);
//            Iterator<ImageReader> itr = ImageIO.getImageReaders(InputStream);
////            while (itr.hasNext()) {
//                ImageReader reader = itr.next();
//                String imageName = reader.getClass().getSimpleName();
//                if (imageName != null) {
//                    if ("JPEGImageReader".equalsIgnoreCase(imageName)){
//                        System.out.println("jpeg");
//                    }
//                    if ("JPGImageReader".equalsIgnoreCase(imageName)) {
////                        return "jpg";
//                        System.out.println("jpg");
//                    }
//                    if ("pngImageReader".equalsIgnoreCase(imageName)) {
////                        return "png";
//                        System.out.println("png");
//                    }
////                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    public static void main(String[] args) throws IOException {
//        String s = "";
//        //        String Path = "C:\\photo";
////        String path = "C:\\photo\\159427146970.jpg";
////        String path = "C:\\photo\\1.png";
////        String imageType = getImageType(s);
////        System.out.println(imageType);
//        String s1 = checkImageBase64Format(s);
////        System.out.println(s1);
////        convertBase64DataToImage(s,Path);
////        convertImageToBase64Data(path);
//
//    }
//
//
//    public static String checkImageBase64Format(String base64ImgData) {
////        byte[] b = Base64.getDecoder().decode(base64ImgData);
//        byte[] b = Base64Utils.decodeFromString(base64ImgData);
//
//        String type = "";
//        if (0x424D == ((b[0] & 0xff) << 8 | (b[1] & 0xff))) {
//            type = "bmp";
//        } else if (0x8950 == ((b[0] & 0xff) << 8 | (b[1] & 0xff))) {
//            type = "png";
//        } else if (0xFFD8 == ((b[0] & 0xff) << 8 | (b[1] & 0xff))) {
//            type = "jpg";
//        }
//        System.out.println(type);
//        return type;
//    }
//
//
//    public static void convertBase64DataToImage(String base64ImgData, String filePath) throws IOException {
//        Files.write(Paths.get(filePath), Base64.getDecoder().decode(base64ImgData), StandardOpenOption.CREATE);
//    }
//
//    public static String convertImageToBase64Data(String filePath) throws IOException {
//        byte[] b = Files.readAllBytes(Paths.get(filePath));
//        String string = Base64.getEncoder().encodeToString(b);
//        System.out.println(string);
//        return Base64.getEncoder().encodeToString(b);
//    }


}



//
//    /**
//     * 上传图片到阿里云
//     * 添加商品图片(上传图片)
//     */
//    public String uploadImgOss(HttpServletRequest request) {
//        String imgurl="";
//        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
//        MultipartFile file = multipartRequest.getFile("image");
//        if(file==null){
//            throw new BusinessException(CommonErrorCode.FAIL);
//        }
//        String ossObjectNamePrefix = AliOSSUtil.APP_SYS_IMAGETEXT+ "-";
//        String ossObjectName = "";
//        int i=1;
//        if(file!=null){
//            String fileName = file.getOriginalFilename();
//            String prefix=fileName.substring(fileName.lastIndexOf("."));
//            fileName = System.currentTimeMillis()+i+prefix;
//
//            ossObjectName = ossObjectNamePrefix + fileName;
//            AliOSSUtil aliOSSUtil = new AliOSSUtil();
//            try {
//                aliOSSUtil.uploadStreamToOss(ossObjectName,file.getInputStream());
//            } catch (IOException e1) {
//                e1.printStackTrace();
//            }
//            String fileUrl = aliOSSUtil.getFileUrl(ossObjectName);
//            imgurl = imgurl + fileUrl.substring(0,fileUrl.lastIndexOf("?")) + ",";
//        }
//        return imgurl.substring(0, imgurl.length()-1);
//    }


//        String url = this.uploadImgOss(request);
//        byte[] bytes = url.getBytes("utf-8");
//        String pic = Base64.getEncoder().encode(bytes).toString();

//        pictureVO.setPic(pic);


//        String result = null;
//        try {
//            result = SdkUtil.post(pictureVO, routeUploadImg);
//        } catch (BusinessException businessException) {
//            throw new BusinessException(CommonErrorCode.E_301002);//暂定
//        }

//        try {
//            Object data = apiClient.uploadImg(pictureVO).getData();
//
//        } catch (BusinessException businessException) {
//            throw new BusinessException(CommonErrorCode.E_301002);//暂定
//        }

//        JSONObject jsonObject = JSONObject.fromObject(result);
//        String imgCode = jsonObject.get("imgCode").toString();

//        merchantImg.setImgCode(imgCode);
//        merchantImg.setImgUrl(url);
//        merchantImg.setPlatform("hx");


//    /**
//     * 商户入网信息查询
//     */
//    @Override
//    public JSONObject queryMcht(String id) throws IOException {
//        if (StringUtils.isEmpty(id)){
//            throw new BusinessException(CommonErrorCode.E_301003);//暂定
//        }
//        MerchantInfo merchantInfo = merchantInfoMapper.selectById(id);
//        String platformId = merchantInfo.getPlatformId();
//        log.info("--------商户入网信息查询--------");
//        Map<String, Object> paramsData = new HashMap<>();
//
//        paramsData.put("instId", "015001");
//        paramsData.put("mchtNo", id);
//        paramsData.put("instMchtNo", platformId);
//        String result = null;
//        try {
//            result = SdkUtil.post(paramsData, routeQueryMcht);
//        } catch (BusinessException businessException) {
//            throw new BusinessException(CommonErrorCode.E_301002);//暂定
//        }
//        JSONObject jsonObject = JSONObject.fromObject(result);
//        return jsonObject;
//    }
//
//
//    /**
//     * 查询商户审核状态
//     */
//    @Override
//    public JSONObject queryMchtAudit(String id) throws IOException {
//        if (StringUtils.isEmpty(id)){
//            throw new BusinessException(CommonErrorCode.E_301003);//暂定
//        }
//        MerchantInfo merchantInfo = merchantInfoMapper.selectById(id);
//        String platformId = merchantInfo.getPlatformId();
//        log.info("--------查询商户审核状态--------");
//        Map<String, Object> paramsData = new HashMap<>();
//        paramsData.put("instId", "015001");
//        paramsData.put("mchtNo", platformId);
//        String result = null;
//        try {
//            result = SdkUtil.post(paramsData, routeQueryMchtAudit);
//        } catch (BusinessException businessException) {
//            throw new BusinessException(CommonErrorCode.E_301002);//暂定
//        }
//        JSONObject jsonObject = JSONObject.fromObject(result);
//        return jsonObject;
//    }
/**
 //     * 商户信息入网
 //     */
//    @Override
//    public String create(MerchantInfoVO merchantInfoVO) throws Exception {
//        if(StringUtils.isEmpty(String.valueOf(merchantInfoVO))){
//            throw new BusinessException(CommonErrorCode.E_301009);//填写信息为空
//        }
//        MerchantInfo merchantInfo = new MerchantInfo();
//        merchantInfo.setSn(merchantInfoVO.getSn());
//        merchantInfo.setSnModelId(merchantInfoVO.getSnModelId());
//        merchantInfo.setServiceType(merchantInfoVO.getServiceType());
//        merchantInfo.setCreateTime(LocalDateTime.now());
//        merchantInfo.setStatus("0");
//
//
//
//        MchtBase mchtBase = merchantInfoVO.getMchtBase();
//        merchantInfo.setId(mchtBase.getInstMchtNo());
//        merchantInfo.setMerchantName(mchtBase.getMchtName());
//        merchantInfo.setSimpleName(mchtBase.getSimpleName());
//        merchantInfo.setMerchantKind(mchtBase.getMchtKind());
//        merchantInfo.setAreaNo(mchtBase.getAreaNo());
//        merchantInfo.setAddress(mchtBase.getAddress());
//        merchantInfo.setMerchantPhone(mchtBase.getStorePhone());
//        merchantInfo.setMerchantScope(mchtBase.getMchtScope());
//        merchantInfo.setMerchantType(mchtBase.getMchtType());
//        merchantInfo.setUnionpayMerchant(mchtBase.getNuionpayMacht());
//
//        MchtUser mchtUser = merchantInfoVO.getMchtUser();
//        merchantInfo.setLegalPersonEmail(mchtUser.getEmail());
//        merchantInfo.setLegalPersonName(mchtUser.getName());
//        merchantInfo.setLegalPersonPhone(mchtUser.getPhone());
//        merchantInfo.setLegalPersonCardno(mchtUser.getCardNo());
//        merchantInfo.setLegalPersonCardnoDate(mchtUser.getCardDate());
//
//        MchtAcct mchtAcct = merchantInfoVO.getMchtAcct();
//        merchantInfo.setAcctProxy(mchtAcct.getAcctProxy());
//        merchantInfo.setAgentCardNo(mchtAcct.getAgentCardNo());
//        merchantInfo.setAgentCardDate(mchtAcct.getAgentCardDate());
//        merchantInfo.setAcctType(mchtAcct.getAcctType());
//        merchantInfo.setAcctNo(mchtAcct.getAcctNo());
//        merchantInfo.setAcctName(mchtAcct.getAcctName());
//        merchantInfo.setAcctBankNo(mchtAcct.getAcctBankNo());
//        merchantInfo.setAcctZbankCode(mchtAcct.getAcctZbankCode());
//        merchantInfo.setAcctZbankNo(mchtAcct.getAcctZbankNo());
//
//        MchtComp mchtComp = merchantInfoVO.getMchtComp();
//        merchantInfo.setLicenseDate(mchtComp.getLicenseDate());
//        merchantInfo.setLicenseNo(mchtComp.getLicenseNo());
//        merchantInfo.setLicenseType(mchtComp.getLicenseType());
//
//
////        //todo 修改  商户开通产品之后才有？？？
////        Map<String, String> product = merchantInfoVO.getProduct();
////        merchantInfo.setProductCode(product.keySet().toString());
////        merchantInfo.setModelId(product.values().toString());
//
////        try {
////                List<MerchantImg> merchantImgs = merchantImgMapper.selectList(new LambdaQueryWrapper<MerchantImg>().eq(MerchantImg::getUserId, mchtBase.getInstMchtNo()));
////            Long count = merchantImgs.stream().map(merchantImg -> {
////                String type = merchantImg.getType();
////                return type;
////            }).distinct().count();
////            if (count < 14){
////                throw new BusinessException(CommonErrorCode.E_301006);//暂定
////            }else if (count < 18){
////                merchantImgs.forEach(merchantImg -> {
////                    String type = merchantImg.getType();
////                    if (type == "03" || type == "06" || type == "07" || type == "08" || type == "03" || type == "17" || type == "18"){
////                        throw new BusinessException(CommonErrorCode.E_301006);//暂定
////                    }
////                });
////            }
////
//////                merchantInfoMapper.updateById(merchantInfo);
////        } catch (BusinessException businessException) {
////            throw new BusinessException(CommonErrorCode.E_301006);//暂定
////        }
//
//        //todo 异常处理
//        Object data = apiClient.createOrgMcht(merchantInfoVO).getData();
//        if (ObjectUtils.equals(data, null)) {
//            throw new BusinessException(CommonErrorCode.E_301010);
//        }
//        JSONObject jsonObject = JSONObject.fromObject(data);
//
//        String desc = jsonObject.getString("desc");
//        //todo 处理返回值
//        if (StringUtils.isNotEmpty(desc)){
//            return desc;
//        }
//        String mchtNo = jsonObject.getString("mchtNo");
//        merchantInfo.setPlatformId(mchtNo);
//        String unionPayMchtNo = jsonObject.getString("unionPayMchtNo");
//        if (StringUtils.isNotEmpty(unionPayMchtNo)) {
//            merchantInfo.setUnionPayMchtNo(unionPayMchtNo);
//        }
//        //存入redis
//        redisTemplate.opsForValue().set(merchantInfo.getId().toString(), mchtNo);
//        merchantInfoMapper.insert(merchantInfo);
//        return mchtNo;
//
//    }
































