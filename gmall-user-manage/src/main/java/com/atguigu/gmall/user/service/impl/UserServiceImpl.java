package com.atguigu.gmall.user.service.impl;


import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.user.mapper.UserAddressMapper;
import com.atguigu.gmall.user.mapper.UserMapper;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;


import com.alibaba.dubbo.config.annotation.Service;
import org.springframework.util.DigestUtils;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    public String userKey_prefix = "user:";
    public String userinfoKey_suffix = ":info";
    public int userKey_timeOut = 60 * 60 * 24;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    UserMapper userMapper;

    @Autowired
    UserAddressMapper userAddressMapper;


    //验证用户
    @Override
    public UserInfo verify(String userId) {
        Jedis jedis = redisUtil.getJedis();
        String key = userKey_prefix + userId + userinfoKey_suffix;
        String usrJson = jedis.get(key);
        if (usrJson != null) {
            //如果能匹配到，加时间
            jedis.expire(key, userKey_timeOut);
            UserInfo userInfo = JSON.parseObject(usrJson, UserInfo.class);
            return userInfo;
        }
        return null;
    }


    @Override
    public UserInfo login(UserInfo userInfo) {
        String passwd = userInfo.getPasswd();
        String passwdmds = DigestUtils.md5DigestAsHex(passwd.getBytes());
        userInfo.setPasswd(passwdmds);
        UserInfo info = userMapper.selectOne(userInfo);
        if (info != null) {
            // 获得到redis ,将用户存储到redis中
            Jedis jedis = redisUtil.getJedis();
            jedis.setex(userKey_prefix + info.getId() + userinfoKey_suffix, userKey_timeOut
                    , JSON.toJSONString(info));
            jedis.close();
            return info;
        }
        return null;
    }


    @Override
    public List<UserInfo> getUserInfoListAll() {
        List<UserInfo> userInfoList = userMapper.selectAll();
        return userInfoList;
    }

    public UserInfo getUserInfo(String id) {
        UserInfo userInfo = userMapper.selectByPrimaryKey(id);
        return userInfo;
    }

    @Override
    public UserInfo getUserInfoById(String id) {

        return userMapper.selectByPrimaryKey(id);
    }

    @Override
    public void addUser(UserInfo userInfo) {
        userMapper.insert(userInfo);
    }

    @Override
    public void updateUser(UserInfo userInfo) {
        userMapper.updateByPrimaryKeySelective(userInfo);
    }

    @Override
    public void updateUserByName(String name, UserInfo userInfo) {

        Example example = new Example(UserInfo.class);
        example.createCriteria().andEqualTo("name", name);
        userMapper.updateByExample(userInfo, example);

    }

    @Override
    public void delUser(UserInfo userInfo) {
        userMapper.deleteByPrimaryKey(userInfo);
    }


    @Override
    public  List<UserAddress> getUserAddressList(String userId){

        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);
        List<UserAddress> userAddressList = userAddressMapper.select(userAddress);
        return  userAddressList;
    }


}
