package com.it.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.dubbo.qos.command.impl.Ls;
import com.alibaba.fastjson.JSON;
import com.it.bean.CartInfo;
import com.it.bean.SkuInfo;
import com.it.gmall.cart.constant.CartConst;
import com.it.gmall.cart.mapper.CartInfoMapper;
import com.it.gmall.config.RedisUtil;
import com.it.service.CartService;
import com.it.service.ManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Reference
    private ManagerService managerService;

    @Autowired
    private RedisUtil redisUtil;
    /**
     * 登录时添加购物车
     */
    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {
        // 构建key user:userid:cart
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        // 准备取数据
        Jedis jedis = redisUtil.getJedis();


        /*先通过skuId,userId查看是否有相同商品*/
        CartInfo cartInfo = new CartInfo();
        cartInfo.setSkuId(skuId);
        cartInfo.setUserId(userId);
        CartInfo cartInfoExist  = cartInfoMapper.selectOne(cartInfo);
        /*有相同的商品*/
        if(cartInfoExist!=null){
            /*数量加*/
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);
            /*给价格进行初始化操作*/
            cartInfoExist.setSkuPrice(cartInfoExist.getCartPrice());
            /*更新购物车*/
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);
        }else {
            /*没有相同的商品 插入到购物车*/
            SkuInfo skuInfo = managerService.getSkuInfo(skuId);
            CartInfo cartInfo1 = new CartInfo();
            /*设置购物车商品参数 属性赋值*/
            cartInfo1.setSkuId(skuId);
            cartInfo1.setCartPrice(skuInfo.getPrice());
            cartInfo1.setSkuPrice(skuInfo.getPrice());
            cartInfo1.setSkuName(skuInfo.getSkuName());
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo1.setUserId(userId);
            cartInfo1.setSkuNum(skuNum);
            /*添加到数据库*/
            cartInfoMapper.insertSelective(cartInfo1);
            cartInfoExist = cartInfo1;
            /*同步缓存*/
        }

        /*讲数据放入缓存*/
        jedis.hset(userCartKey, skuId, JSON.toJSONString(cartInfoExist));

        /**
         * 面试时不懂过期可以不说
         */
        /*设置过期时间*/
        /*获取用户的key*/
        String userKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USERINFOKEY_SUFFIX;
        /*获取userKey的过期时间*/
        Long ttl = jedis.ttl(userKey);
        /*保持购物车和用户的过期时间一致*/
        jedis.expire(userCartKey,ttl.intValue());
        jedis.close();
    }

    @Override
    public List<CartInfo> getCartList(String userId) {
        /*
        * 1.从缓存中获取数据
       2.缓存中没有数据时，从数据库获取数据
        * */
        // 构建key user:userid:cart
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        // 准备取数据
        Jedis jedis = redisUtil.getJedis();
        List<CartInfo> cartInfoList = new ArrayList<>();
        /*获取到购物车中Json形式的商品集合*/
        List<String> stringList = jedis.hvals(userCartKey);
        if(stringList!=null && stringList.size()>0){
            for (String cartInfo : stringList) {
                /*获取到购物车商品对象*/
                cartInfoList.add(JSON.parseObject(cartInfo, CartInfo.class));
            }
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            });
            return cartInfoList;
        }else{
            // 从数据库中查询，其中cart_price 可能是旧值，所以需要关联sku_info 表信息。
            cartInfoList = loadCartCache(userId);
            return  cartInfoList;
        }
    }

    /**
     * 合并购物车(只可以在登录状态下合并)
     * @param cartListCK
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId) {
        /*获取数据库中的购物车*/
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);
        for (CartInfo cartInfoCK : cartListCK){
            boolean isMatch =false;
            for(CartInfo cartInfo : cartInfoList){
                /*匹配上*/
            if(cartInfo.getSkuId().equals(cartInfoCK.getSkuId())){
                cartInfo.setSkuNum(cartInfo.getSkuNum()+cartInfoCK.getSkuNum());
                cartInfoMapper.updateByPrimaryKeySelective(cartInfo);
                isMatch = true;
                }
            }
                /*没有匹配上*/
            if(!isMatch){
                cartInfoCK.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfoCK);
            }
        }
        /*重新查数据库*/
        List<CartInfo> cartInfoList1 = loadCartCache(userId);

        /*未登录状态下勾选的合并*/
        for(CartInfo cartInfo : cartInfoList1){
            for(CartInfo cartInfoCK : cartListCK){
                if(cartInfo.getSkuId().equals(cartInfoCK.getSkuId())){
                    /*只有被勾选的时候进行更改*/
                    if(cartInfoCK.getIsChecked().equals("1")){
                        cartInfo.setIsChecked("1");
                        /*更新redis的ischecked*/
                        checkCart(cartInfo.getSkuId(),"1",userId);
                    }
                }
            }
        }
        return cartInfoList1;
    }

    @Override
    public void checkCart(String skuId, String isChecked, Object userId) {
        /*
        * 1.获取jedis客户端
        * 2.获取购物车
        * 3.直接修改skuId商品的isCheck状态
        * 4.写回购物车
        * 5、新建一个购物车存储勾选转态
        * */
        Jedis jedis = redisUtil.getJedis();
        // 构建key user:userid:cart
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        /*获取选中的商品*/
        String hget = jedis.hget(userCartKey, skuId);
        /*转换为对象*/
        CartInfo cartInfo = JSON.parseObject(hget, CartInfo.class);
        cartInfo.setIsChecked(isChecked);
        /*写回购物车*/
        jedis.hset(userCartKey,skuId,JSON.toJSONString(cartInfo));
        /*新建购物车 user:userid:checked*/
        String userCartKeyCheck = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CHECKED_KEY_SUFFIX;
        if("1".equals(isChecked)){
            jedis.hset(userCartKeyCheck,skuId,JSON.toJSONString(cartInfo));
        }else{
            jedis.hdel(userCartKeyCheck,skuId,JSON.toJSONString(cartInfo));
        }
        jedis.close();
    }

    /**
     * 送货清单(选中的购物车)
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        Jedis jedis = redisUtil.getJedis();
        /*被选中的购物车*/
        String userCartCheckKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CHECKED_KEY_SUFFIX;
        List<CartInfo> cartInfoList = new ArrayList<>();
        /*获取redis中被选中的购物车集合*/
        List<String> cartInfoCheckList = jedis.hvals(userCartCheckKey);
        if(cartInfoCheckList != null && cartInfoCheckList.size()>0){
            for(String cartInfoChec : cartInfoCheckList){
                cartInfoList.add(JSON.parseObject(cartInfoChec, CartInfo.class));
            }
        }
        jedis.close();
        return cartInfoList;
    }

    @Override
    public List<CartInfo> loadCartCache(String userId) {
        /*从数据库中查询购物车商品数据*/
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);
        /*如果数据库中也查不到 购物车为空*/
        if(cartInfoList==null && cartInfoList.size() == 0){
            return null;
        }
        // 构建key user:userid:cart
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        // 准备取数据
        Jedis jedis = redisUtil.getJedis();
        /*能够走到这里  说明有数据*/
        for(CartInfo cartInfo : cartInfoList){
            jedis.hset(userCartKey,cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
        }
        jedis.close();
        return cartInfoList;
    }
}
