package com.atguigu.yygh.msm.service.impl;

import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.msm.service.MsmService;
import com.atguigu.yygh.msm.utils.HttpUtils;
import com.atguigu.yygh.msm.utils.RandomUtil;
import com.atguigu.yygh.vo.msm.MsmVo;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class MsmServiceImpl implements MsmService {

    @Override
    public boolean send(MsmVo msmVo) {
        if(!StringUtils.isEmpty(msmVo.getPhone())) {
            String code = RandomUtil.getFourBitRandom();
            return this.sendMessage(msmVo.getPhone(),code);
        }
        return false;
    }

    @Override
    public boolean sendMessage(String phone, String code) {
        String host = "https://cxkjsms.market.alicloudapi.com";
        String path = "/chuangxinsms/dxjk";
        String method = "POST";
        String appcode = "9d8f9a2348fe4a639df2a26e9b38710c";//开通服务后 买家中心-查看AppCode
        Map<String, String> headers = new HashMap<String, String>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);
        Map<String, String> querys = new HashMap<String, String>();
        String messageContent = "【智慧医疗系统】您的验证码是：" + code + "，5分钟内有效，请勿泄露。";
        querys.put("content", messageContent);
        querys.put("mobile", phone);
        Map<String, String> bodys = new HashMap<String, String>();
        try {
            /**
             * 重要提示如下:
             * HttpUtils请从
             * https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/src/main/java/com/aliyun/api/gateway/demo/util/HttpUtils.java
             * 下载
             *
             * 相应的依赖请参照
             * https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/pom.xml
             */
            HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
            System.out.println(response.toString());
            //获取response的body
            //System.out.println(EntityUtils.toString(response.getEntity()));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


//    //TODO 仅为了测试
//    //发送短信实现
//    @Override
//    public boolean send(MsmVo msmVo) {
//        if (!StringUtils.isEmpty(msmVo.getPhone())) {
//            String code = RandomUtil.getFourBitRandom();
//            return this.sendMessage(msmVo.getPhone(), code);
//        }
//        return false;
//    }

}

