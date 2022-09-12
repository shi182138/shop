package com.example.shop.product.feign;




import com.example.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("shop-ware")
public interface WareFeignService {

    @PostMapping("/ware/waresku/hasstock")
    R getHasStock(@RequestBody List<Long> skuIds);
}
