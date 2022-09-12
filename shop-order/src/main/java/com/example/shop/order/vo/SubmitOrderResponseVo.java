package com.example.shop.order.vo;

import com.example.shop.order.entity.OrderEntity;
import lombok.Data;


@Data
public class SubmitOrderResponseVo {

    private OrderEntity order;

    /** 错误状态码 **/
    private Integer code;


}
