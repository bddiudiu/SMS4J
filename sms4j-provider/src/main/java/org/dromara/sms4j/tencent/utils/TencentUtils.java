package org.dromara.sms4j.tencent.utils;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.dromara.sms4j.tencent.config.TencentConfig;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;


/**
 * @author Richard
 * @date 2023-04-18 19:50
 */
@Slf4j
public class TencentUtils {
    /**
     * 加密方式
     */
    private static final String ALGORITHM = "TC3-HMAC-SHA256";
    /**
     * 请求方式
     */
    private static final String HTTP_REQUEST_METHOD = "POST";

    private static final String CT_JSON = "application/json; charset=utf-8";


    private static byte[] hmac256(byte[] key, String msg) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, mac.getAlgorithm());
        mac.init(secretKeySpec);
        return mac.doFinal(msg.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256Hex(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
        return DatatypeConverter.printHexBinary(d).toLowerCase();
    }

    /**
     * 生成腾讯云发送短信接口签名
     *
     * @param templateId 模板id
     * @param messages   短信内容
     * @param phones     手机号
     * @param timestamp  时间戳
     * @throws Exception
     */
    public static String generateSignature(TencentConfig tencentConfig, String templateId, String[] messages, String[] phones,
                                           String timestamp) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String date = sdf.format(new Date(Long.parseLong(timestamp + "000")));
        String canonicalUri = "/";
        String canonicalQueryString = "";
        String canonicalHeaders = "content-type:application/json; charset=utf-8\nhost:" + tencentConfig.getRequestUrl() + "\n";
        String signedHeaders = "content-type;host";
        Map<String, Object> params = new HashMap<>();
        params.put("PhoneNumberSet", phones);
        params.put("SmsSdkAppId", tencentConfig.getSdkAppId());
        params.put("SignName", tencentConfig.getSignature());
        params.put("TemplateId", templateId);
        params.put("TemplateParamSet", messages);
        String payload = JSON.toJSONString(params);
        String hashedRequestPayload = sha256Hex(payload);
        String canonicalRequest = HTTP_REQUEST_METHOD + "\n" + canonicalUri + "\n" + canonicalQueryString + "\n" + canonicalHeaders + "\n" + signedHeaders + "\n" + hashedRequestPayload;
        String credentialScope = date + "/" + tencentConfig.getService() + "/tc3_request";
        String hashedCanonicalRequest = sha256Hex(canonicalRequest);
        String stringToSign = ALGORITHM + "\n" + timestamp + "\n" + credentialScope + "\n" + hashedCanonicalRequest;
        byte[] secretDate = hmac256(("TC3" + tencentConfig.getAccessKeySecret()).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmac256(secretDate, tencentConfig.getService());
        byte[] secretSigning = hmac256(secretService, "tc3_request");
        String signature = DatatypeConverter.printHexBinary(hmac256(secretSigning, stringToSign)).toLowerCase();
        return ALGORITHM + " Credential=" + tencentConfig.getAccessKeyId() + "/" + credentialScope + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature;
    }

    /**
     * 生成腾讯云短信请求头map
     *
     * @param authorization 签名信息
     * @param timestamp     时间戳
     * @param action        接口名称
     * @param version       接口版本
     * @param territory     服务器地区
     * @param requestUrl    请求地址
     */
    public static Map<String, Object> generateHeadsMap(String authorization, String timestamp, String action,
                                                       String version, String territory, String requestUrl) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Authorization", authorization);
        headers.put("Content-Type", CT_JSON);
        headers.put("Host", requestUrl);
        headers.put("X-TC-Action", action);
        headers.put("X-TC-Timestamp", timestamp);
        headers.put("X-TC-Version", version);
        headers.put("X-TC-Region", territory);
        return headers;
    }

    /**
     * 生成腾讯云短信请求body
     *
     * @param phones           手机号
     * @param sdkAppId         appid
     * @param signatureName    短信签名
     * @param templateId       模板id
     * @param templateParamSet 模板参数
     * @return
     */
    public static Map<String, Object> generateRequestBody(String[] phones, String sdkAppId, String signatureName,
                                                          String templateId, String[] templateParamSet) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("PhoneNumberSet", phones);
        requestBody.put("SmsSdkAppId", sdkAppId);
        requestBody.put("SignName", signatureName);
        requestBody.put("TemplateId", templateId);
        requestBody.put("TemplateParamSet", templateParamSet);
        return requestBody;
    }

}