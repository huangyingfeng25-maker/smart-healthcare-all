package com.atguigu.yygh.orders.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.result.ResultCodeEnum;
import com.atguigu.yygh.enums.OrderStatusEnum;
import com.atguigu.yygh.hosp.client.HospitalFeignClient;
import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.model.user.Patient;
import com.atguigu.yygh.orders.mapper.OrderInfoMapper;
import com.atguigu.yygh.orders.service.OrderInfoService;
import com.atguigu.yygh.orders.utils.HttpRequestHelper;
import com.atguigu.yygh.rabbit.RabbitService;
import com.atguigu.yygh.rabbit.constant.MqConst;
import com.atguigu.yygh.user.client.PatientFeignClient;
import com.atguigu.yygh.vo.hosp.ScheduleOrderVo;
import com.atguigu.yygh.vo.msm.MsmVo;
import com.atguigu.yygh.vo.order.OrderMqVo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 订单表 服务实现类
 */
@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private PatientFeignClient patientFeignClient;

    @Autowired
    private HospitalFeignClient hospitalFeignClient;
    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private RabbitService rabbitService;

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        orderInfo.getParam().put("orderStatusString",
                OrderStatusEnum.getStatusNameByStatus(orderInfo.getOrderStatus()));
        return orderInfo;
    }

    //平台下单
    @Override
    public Long createOrder(String scheduleId, Long patientId) {

        //1.远程调用用户微服务:根据就诊人id获取就诊人信息Patient
        Patient patient = patientFeignClient.getPatientInfoById(patientId);//就诊人数据

        //2.远程调用医院微服务:根据排班id获取ScheduleOrderVo
        ScheduleOrderVo scheduleOrderVo = hospitalFeignClient.getScheduleOrderVo(scheduleId);

        //3.封装请求参数，远程调用医院挂号接口，完成下单

        //3.1封装参数
        Map<String,Object> paramMap=new HashMap<>();
        paramMap.put("hoscode",scheduleOrderVo.getHoscode());//医院编码
        paramMap.put("depcode",scheduleOrderVo.getDepcode());//课室编码
        paramMap.put("hosScheduleId",scheduleOrderVo.getHosScheduleId());//医院提供排班id
        paramMap.put("reserveDate",new DateTime(scheduleOrderVo.getReserveDate()).toString("yyyy-MM-dd"));//预约挂号日期
        paramMap.put("reserveTime", scheduleOrderVo.getReserveTime());//预约挂号时间
        paramMap.put("amount",scheduleOrderVo.getAmount()); //挂号费用 0-上午 1-下午

        //就诊人
        paramMap.put("name", patient.getName());//就诊人名称
        paramMap.put("certificatesType",patient.getCertificatesType());//证件类型 身份证10 户口本20
        paramMap.put("certificatesNo", patient.getCertificatesNo());//实名认证的证件号
        paramMap.put("sex",patient.getSex());//性别
        paramMap.put("birthdate", patient.getBirthdate());//生日
        paramMap.put("phone",patient.getPhone());//手机号
        paramMap.put("isMarry", patient.getIsMarry());//是否结婚
        paramMap.put("provinceCode",patient.getProvinceCode());//省编码
        paramMap.put("cityCode", patient.getCityCode());//市编码
        paramMap.put("districtCode",patient.getDistrictCode());//区编码
        paramMap.put("address",patient.getAddress());//联系地址

        //就诊人对应联系人
        paramMap.put("contactsName",patient.getContactsName());//联系人名称
        paramMap.put("contactsCertificatesType", patient.getContactsCertificatesType());//联系人的证件类型身份证10、户口本20
        paramMap.put("contactsCertificatesNo",patient.getContactsCertificatesNo());//实名认证的证件号码
        paramMap.put("contactsPhone",patient.getContactsPhone());//联系人手机号
        paramMap.put("timestamp", HttpRequestHelper.getTimestamp());//下单时间
        //String sign = HttpRequestHelper.getSign(paramMap, signInfoVo.getSignKey());
        paramMap.put("sign", "");//接口权限校验的签名：相当于密钥

        //3.2远程接口调用
        JSONObject result = HttpRequestHelper.sendRequest(paramMap, "http://localhost:9998/order/submitOrder");

        //4.判断医院挂号是否成功
        if(result.getInteger("code") == 200) {

        //4.1挂号成功
            JSONObject jsonObject = result.getJSONObject("data");
            //5.保存订单
            OrderInfo orderInfo=new OrderInfo();
            //设置添加数据--排班数据
            BeanUtils.copyProperties(scheduleOrderVo, orderInfo);
            //设置添加数据--就诊人数据
            //订单号
            String outTradeNo = System.currentTimeMillis() + ""+ new Random().nextInt(100);
            orderInfo.setOutTradeNo(outTradeNo);
            orderInfo.setScheduleId(scheduleOrderVo.getHosScheduleId());
            orderInfo.setPatientId(patientId);
            orderInfo.setPatientName(patient.getName());
            orderInfo.setPatientPhone(patient.getPhone());
            orderInfo.setHosRecordId(jsonObject.getInteger("hosRecordId").toString());
            orderInfo.setNumber(jsonObject.getInteger("number"));
            orderInfo.setFetchTime(jsonObject.getString("fetchTime"));
            orderInfo.setFetchAddress(jsonObject.getString("fetchAddress"));
            orderInfo.setOrderStatus(OrderStatusEnum.UNPAID.getStatus());//0 下单未支付  1已支付  2取号  -1取消挂号
            orderInfoMapper.insert(orderInfo);//主键回填


            //系统优化：6和7步骤，需要进行异步操作，大大提高系统响应速度。借助Mq

            //发送mq信息更新号源和短信通知
            OrderMqVo orderMqVo = new OrderMqVo();
            orderMqVo.setScheduleId(scheduleId);
            orderMqVo.setReservedNumber(jsonObject.getInteger("reservedNumber"));
            orderMqVo.setAvailableNumber(jsonObject.getInteger("availableNumber"));

            //短信提示
            MsmVo msmVo = new MsmVo();
            msmVo.setPhone(orderInfo.getPatientPhone());
            String reserveDate =
                    new DateTime(orderInfo.getReserveDate()).toString("yyyy-MM-dd")
                            + (orderInfo.getReserveTime()==0 ? "上午": "下午");
            Map<String,Object> param = new HashMap<String,Object>(){{
                put("title", orderInfo.getHosname()+"|"+orderInfo.getDepname()+"|"+orderInfo.getTitle());
                put("amount", orderInfo.getAmount());
                put("reserveDate", reserveDate);
                put("name", orderInfo.getPatientName());
                put("quitTime", new DateTime(orderInfo.getQuitTime()).toString("yyyy-MM-dd HH:mm"));
            }};//匿名内部类的动态代码块

            msmVo.setParam(param);
            orderMqVo.setMsmVo(msmVo);

            //6.修改排班可预约数量 mongo
            //7.发送短信信息提醒
            rabbitService.sendMwssage(MqConst.EXCHANGE_DIRECT_ORDER,MqConst.ROUTING_ORDER,orderMqVo);//6和7步骤由消费者端来异步处理。
            //返回订单号
            return orderInfo.getId();
        } else { //挂号失败
            //throw new YyghException(20001,result.getString("msg"));
            throw new YyghException(20001,"挂号失败");
        }
    }
}
