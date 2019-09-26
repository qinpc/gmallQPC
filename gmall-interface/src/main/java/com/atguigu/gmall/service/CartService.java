package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.CartInfo;

import java.util.List;

public interface CartService {

    CartInfo addCart(String userId, String skuId, Integer num);

    List<CartInfo> cartList(String userId);

    List<CartInfo> mergeCartList(String userIdDest ,String userIdOrig);

    void checkCart(String userId, String skuId, String isChecked);

    List<CartInfo>  getCheckedCartList(String userId);
}
