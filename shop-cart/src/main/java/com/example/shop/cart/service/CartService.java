package com.example.shop.cart.service;


import com.example.shop.cart.vo.CartItemVo;
import com.example.shop.cart.vo.CartVo;

import java.util.List;
import java.util.concurrent.ExecutionException;


public interface CartService {


//    void addToCart(Long skuId, Integer num);

    CartItemVo addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    CartVo getCart() throws ExecutionException, InterruptedException;

    /**
     * 清空购物车的数据
     * @param cartKey
     */
    public void clearCartInfo(String cartKey);

    void checkItem(Long skuId, Integer checked);

    public CartItemVo getCartItem(Long skuId);

    void changeItemCount(Long skuId, Integer num);

    void deleteIdCartInfo(Integer skuId);

    List<CartItemVo> getUserCartItems();

}
