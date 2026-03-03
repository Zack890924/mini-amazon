package com.erss.worldconnector.dto;

import lombok.Data;
import java.util.List;

@Data
public class ArrivedRequestDTO {

    private long seqNum;

    private int warehouseId;

    private long packageId;

    private List<ArrivedItemDTO> items;
}
