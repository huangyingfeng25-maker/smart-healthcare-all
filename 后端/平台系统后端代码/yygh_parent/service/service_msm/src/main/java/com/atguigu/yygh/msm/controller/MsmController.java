package com.atguigu.yygh.msm.controller;

import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.common.result.ResultCodeEnum;
import com.atguigu.yygh.msm.service.MsmService;
import com.atguigu.yygh.msm.utils.RandomUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/msm")
public class MsmController {

    @Autowired
    private MsmService msmService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    //根据手机号发送短信验证码
    @PostMapping(value = "/send/{phone}")
    public R code(@PathVariable String phone) {

        String code = redisTemplate.opsForValue().get(phone);
        if(StringUtils.hasText(code)) {//“是否包含真正的文本”
            // 就不往下走了，直接返回成功，骗过前端，防止重复扣费
            return R.ok().message("验证码尚未过期，请注意查收");
        }
        //生成验证码
        code = RandomUtil.getFourBitRandom();
        //调用service方法发送
        boolean is_success = msmService.sendMessage(phone,code);
        //如果发送成功，把验证码放到redis，设置有效时长
        if(is_success) {
            //key：手机号  value：验证码
            redisTemplate.opsForValue().set(phone,code,5, TimeUnit.MINUTES);
            return R.ok();
        } else {
            return R.error().message("短信发送失败");
        }
    }
}
