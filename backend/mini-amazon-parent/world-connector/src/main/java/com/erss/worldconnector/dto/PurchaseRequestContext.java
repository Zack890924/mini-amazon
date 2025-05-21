package com.erss.worldconnector.dto;



import com.erss.common.proto.AProduct;
import lombok.Data;

import java.util.List;


@Data
public class PurchaseRequestContext {
    private int warehouseId;
    private long packageId;
    private List<AProduct> products;

    public PurchaseRequestContext(int warehouseId, long packageId, List<AProduct> products){

        this.warehouseId = warehouseId;
        this.packageId = packageId;
        this.products = products;
    }
}
