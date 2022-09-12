package com.example.shop.ware.vo;

import lombok.Data;
import org.apache.catalina.LifecycleState;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class PurchaseDoneVo {

    @NotNull
    private Long id; //采购单

    private List<PurchaseItemDoneVo> items; //采购项
}
