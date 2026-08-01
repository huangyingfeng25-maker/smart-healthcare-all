package com.atguigu.yygh.common.exception;

import com.atguigu.yygh.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
//@ControllerAdvice(让返回结果变成 JSON) + @ResponseBody
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public R error(Exception e) {
        // 使用 log.error 记录日志，并将异常堆栈对象 e 传入，底层会自动打印堆栈到日志文件中
        log.error("全局异常兜底处理: ", e);
        return R.error().message("执行全局异常处理");
    }

    //2. 特定异常处理（例如数学运算异常）
    @ExceptionHandler(ArithmeticException.class)
    public R error(ArithmeticException e) {
        log.error("发生特定异常(ArithmeticException): ", e);
        return R.error().message("执行特定异常处理");
    }

    //3. 自定义业务异常处理（最常用的）
    @ExceptionHandler(YyghException.class)
    public R error(YyghException e) {
        log.error("执行自定义业务异常处理: 状态码={}, 错误信息={}", e.getCode(), e.getMessage());
        return R.error().code(e.getCode()).message(e.getMsg());
    }
}
