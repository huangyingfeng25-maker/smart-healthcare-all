package com.atguigu.yygh.msm.service;

import com.atguigu.yygh.vo.msm.MsmVo;

public interface MsmService {

    ////根据手机号发送短信验证码
    boolean sendMessage(String phone, String code);

    boolean send(MsmVo msmVo);
}
