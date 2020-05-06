package com.it.service;

import com.it.bean.CartInfo;

import java.util.List;

public interface CartService {
    public  void  addToCart(String skuId,String userId,Integer skuNum);

    List<CartInfo> getCartList(String userId);

    List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId);

    void checkCart(String skuId, String isChecked, Object userId);

    List<CartInfo> getCartCheckedList(String userId);

    List<CartInfo> loadCartCache(String userId);
}
