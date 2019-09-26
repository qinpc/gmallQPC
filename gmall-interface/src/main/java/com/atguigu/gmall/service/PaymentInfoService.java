package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PaymentInfo;

public interface PaymentInfoService {

    void savePaymentInfo(PaymentInfo paymentInfo);

    PaymentInfo getPaymentInfo(PaymentInfo paymentInfo);

    void updatePaymentInfoByOutTradeNo(String outTradeNo, PaymentInfo paymentInfo);

    void sendPaymentToOrder(String orderId, String result);

    void sendDelayPaymentResult(String outTradeNo, Long delaySec, Integer checkCount);
}
