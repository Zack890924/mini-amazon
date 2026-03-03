package com.erss.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OperationContext {
    private String operationType;
    private long packageId;
    private Integer truckId;
}
