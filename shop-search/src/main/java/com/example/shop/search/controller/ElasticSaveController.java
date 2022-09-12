package com.example.shop.search.controller;

import com.example.common.common.BizCodeEnum;
import com.example.common.to.es.SkuEsModel;
import com.example.common.utils.R;
import com.example.shop.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequestMapping("/search")
@RestController
public class ElasticSaveController {

    @Autowired
    ProductSaveService productSaveService;
    //上架商品
    @PostMapping("/save/product")
    public R productStatusUp(@RequestBody List<SkuEsModel> skuEsModels) {

        boolean b = false;
        try {
             b = productSaveService.productStatusUp(skuEsModels);
        } catch (Exception e) {
            log.error("elasticSaveController商品上架错误：{}", e);
            return R.error(
                    BizCodeEnum.PRODUCT_UP_EXCEPTION.getCode(),
                    BizCodeEnum.PRODUCT_UP_EXCEPTION.getMsg()
            );
        }

        if (!b) {
            return R.ok();
        } else {
            return R.error(
                    BizCodeEnum.PRODUCT_UP_EXCEPTION.getCode(),
                    BizCodeEnum.PRODUCT_UP_EXCEPTION.getMsg()
            );
        }

    }
}
