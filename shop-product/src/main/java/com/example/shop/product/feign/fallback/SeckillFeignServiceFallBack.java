package com.example.shop.product.feign.fallback;

import com.example.common.common.BizCodeEnum;
import com.example.common.utils.R;
import com.example.shop.product.feign.SeckillFeignService;
import org.springframework.stereotype.Component;



@Component
public class SeckillFeignServiceFallBack implements SeckillFeignService {
    @Override
    public R getSkuSeckilInfo(Long skuId) {
        return R.error(BizCodeEnum.TO_MANY_REQUEST.getCode(),BizCodeEnum.TO_MANY_REQUEST.getMsg());
    }
}
