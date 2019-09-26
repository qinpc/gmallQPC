package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.*;

@Service
public class CartServiceImpl  implements CartService {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    CartInfoMapper cartInfoMapper;

    @Reference
    ManageService manageService;

    @Override
    public CartInfo addCart(String userId, String skuId, Integer num) {

        // 为了防止更新购物车前缓存过期，添加缓存过期时间
        loadCartCacheIfNotExists(userId) ;
        // 加数据库
        // 尝试取出已有的数据    如果有  把数量更新 update   如果没有insert
        CartInfo cartInfoQuery = new CartInfo();
        cartInfoQuery.setSkuId(skuId);
        cartInfoQuery.setUserId(userId);
        //根据userId查询数据库
        CartInfo cartInfoExists = cartInfoMapper.selectOne(cartInfoQuery);
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        if (cartInfoExists!=null){//数据库有userId数据，
            //更新数据库
            cartInfoExists.setSkuName(skuInfo.getSkuName());
            cartInfoExists.setCartPrice(skuInfo.getPrice());
            cartInfoExists.setSkuNum(cartInfoExists.getSkuNum()+num);
            cartInfoExists.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExists);
        }else{//数据库没有userId数据
            //插入数据库
            CartInfo cartInfo = new CartInfo();
            cartInfo.setSkuId(skuId);
            cartInfo.setUserId(userId);
            cartInfo.setSkuNum(num);
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfoMapper.insertSelective(cartInfo);
            cartInfoExists=cartInfo;
        }

        loadCartCache(userId);

        return cartInfoExists;
    }

    @Override
    public List<CartInfo> cartList(String userId) {
        //先查缓存
        Jedis jedis = redisUtil.getJedis();
        String cartKey="cart:"+userId+":info";
        List<String> cartJsonList = jedis.hvals(cartKey);
        List<CartInfo> cartList=new ArrayList<>();
        if(cartJsonList!=null&&cartJsonList.size()>0){//缓存命中
            for (String cartJson : cartJsonList) {
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
                cartList.add(cartInfo);
            }
            //排序
            cartList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return o2.getId().compareTo(o1.getId());
                }
            });
            return    cartList;
        }else {
            //缓存未命中  直接查数据库 ，同时加载到缓存中
            return loadCartCache(userId);
        }
    }


    //查数据库，同时加载到缓存中
    public List<CartInfo>  loadCartCache(String userId){
        // 读取数据库
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithSkuPrice(userId);
        //加载到缓存中
        //为了方便插入redis  把list --> map
        if(cartInfoList!=null&&cartInfoList.size()>0) {
            Map<String, String> cartMap = new HashMap<>();
            for (CartInfo cartInfo : cartInfoList) {
                cartMap.put(cartInfo.getSkuId(), JSON.toJSONString(cartInfo));
            }
            Jedis jedis = redisUtil.getJedis();
            String cartKey = "cart:" + userId + ":info";
            jedis.del(cartKey);
            jedis.hmset(cartKey, cartMap);                // hash
            jedis.expire(cartKey, 60 * 60 * 24);
            jedis.close();
        }
        return  cartInfoList;

    }


    //登录后合并购物车
    @Override
    public List<CartInfo> mergeCartList(String userIdDest, String userIdOrig) {
        //1 先做合并
        cartInfoMapper.mergeCartList(userIdDest,userIdOrig);
        // 2 合并后把临时购物车删除
        CartInfo cartInfo = new CartInfo();
        cartInfo.setUserId(userIdOrig);
        cartInfoMapper.delete(cartInfo);
        Jedis jedis = redisUtil.getJedis();
        jedis.del("cart:"+userIdOrig+":info");
        jedis.close();
        // 3 重新读取数据 加载缓存
        List<CartInfo> cartInfoList = loadCartCache(userIdDest);

        return cartInfoList;
    }

    //勾选框
    @Override
    public void checkCart(String userId, String skuId, String isChecked) {
        loadCartCacheIfNotExists(userId);//延长缓存数据
        //保存的KEY
        String cartKey="cart:"+userId+":info";
        Jedis jedis = redisUtil.getJedis();
        //取得具体SKU数据
        String cartInfoJson = jedis.hget(cartKey, skuId);
        CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
        //改变选中状态
        cartInfo.setIsChecked(isChecked);
        String cartInfoJsonNew = JSON.toJSONString(cartInfo);
        //存入缓存
        jedis.hset(cartKey,skuId,cartInfoJsonNew);
        //为方便订单取已选中商品缓存，再新建一个hash,把选中物品存入
        String CheckKey = "cart:"+userId+":checked";
        if("1".equals(isChecked)){//"1"的存入新hash,否则删除
            jedis.hset(CheckKey,skuId,cartInfoJsonNew);
            jedis.expire(CheckKey,60*60);
        }else {
            jedis.hdel(CheckKey,skuId);
        }
        jedis.close();

    }


    //延长缓存过期时间
    public void  loadCartCacheIfNotExists(String userId){
        String cartkey="cart:"+userId+":info";
        Jedis jedis = redisUtil.getJedis();
        Long ttl = jedis.ttl(cartkey);
        int ttlInt = ttl.intValue();
        jedis.expire(cartkey,ttlInt+100);
        Boolean exists = jedis.exists(cartkey);
        jedis.close();
        if( !exists){
            loadCartCache( userId);
        }

    }


    @Override
    public List<CartInfo> getCheckedCartList(String userId) {
        String cartCheckedKey = "cart:" + userId + ":checked";
        Jedis jedis = redisUtil.getJedis();

        List<String> checkedCartList = jedis.hvals(cartCheckedKey);
        List<CartInfo> cartInfoList=new ArrayList<>();
        for (String cartInfoJson : checkedCartList) {
            CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
            cartInfoList.add(cartInfo);
        }


        jedis.close();

        return cartInfoList;
    }


}
