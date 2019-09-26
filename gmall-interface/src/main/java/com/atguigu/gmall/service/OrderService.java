package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.enums.ProcessStatus;

public interface OrderService {
    String genToken(String userId);

    String saveOrder(OrderInfo orderInfo);

    boolean verifyToken(String userId, String token);

    OrderInfo getOrderInfo(String orderId);

    void updateStatus(String orderId, ProcessStatus processStatus, OrderInfo... orderInfo );
}
