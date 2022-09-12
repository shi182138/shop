package com.example.shop.ware.vo;

import lombok.Data;

import java.util.List;


@Data
public class WareSkuLockVo {

    private String orderSn;  //订单号

    /** 需要锁住的所有库存信息 **/
    private List<OrderItemVo> locks;



}
