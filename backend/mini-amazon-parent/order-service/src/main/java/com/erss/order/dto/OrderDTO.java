package com.erss.order.dto;

import lombok.Data;
import java.util.Date;

@Data
public class OrderDTO{
    private Long orderId;
    private Date orderDate;
    private String status;
    private String trackingNumber;
    private Long packageId;
    private Integer destinationX;
    private Integer destinationY;
}