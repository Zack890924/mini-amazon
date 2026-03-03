
package com.erss.warehouse.dto;

import lombok.Data;
import java.util.List;

import lombok.Data;

import java.util.List;

@Data
public class ArrivedRequestDTO {
    private long seqNum;
    private long packageId;
    private int  warehouseId;
    private List<ArrivedItemDTO> items;
}
