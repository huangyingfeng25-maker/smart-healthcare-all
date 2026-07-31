package com.atguigu.yygh.msm.service;

public interface MsmService {

    ////根据手机号发送短信验证码
    boolean sendMsm(String phone, String code);
}
