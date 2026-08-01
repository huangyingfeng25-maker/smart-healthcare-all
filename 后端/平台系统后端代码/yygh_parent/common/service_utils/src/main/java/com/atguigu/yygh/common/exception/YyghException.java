package com.atguigu.yygh.common.exception;

import com.atguigu.yygh.common.result.ResultCodeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YyghException extends RuntimeException {

    private Integer code;  //异常状态码

    private String msg;  //异常描述

    // ⭐ 新增：直接接收枚举对象的构造器
    public YyghException(ResultCodeEnum resultCodeEnum) {
        super(resultCodeEnum.getMessage()); // 从枚举里取出信息交给父类
        this.code = resultCodeEnum.getCode(); // 从枚举里取出状态码
    }
}
