package com.erss.order.service;

import com.erss.order.client.WarehouseClient;
import com.erss.order.dto.OrderDTO;
import com.erss.order.dto.PurchaseRequestDTO;
import com.erss.order.dto.PurchaseResponseDTO;
import com.erss.order.entity.Order;
import com.erss.order.repository.OrderRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final WarehouseClient warehouseClient;
    private final OrderRepository orderRepository;

    public PurchaseResponseDTO placeOrder(@Valid PurchaseRequestDTO req) {
        PurchaseResponseDTO response = warehouseClient.purchase(req);


        Long packageId = response.getOrderId();

        log.info("Received packageId: {}", packageId);


        Order order = new Order();
        order.setId(packageId);
        order.setUserId(1L);
        order.setProductId(req.getItems().get(0).getProductId());
        order.setQuantity(req.getItems().get(0).getQuantity());
        order.setDestinationX(req.getDeliveryAddress().getX());
        order.setDestinationY(req.getDeliveryAddress().getY());
        order.setTrackingNumber("UPS" + packageId);
        order.setStatus("Processing");
        order.setUpsAccount(req.getUpsAccount());
        order.setOrderDate(new Date());

        orderRepository.save(order);

        return response;
    }

    public List<OrderDTO> findOrdersByUpsAccount(String upsAccount) {
        List<Order> orders = orderRepository.findByUpsAccount(upsAccount);
        return orders.stream()
                .map(this::convertToOrderDTO)
                .collect(Collectors.toList());
    }

    private OrderDTO convertToOrderDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setOrderId(order.getId());
        dto.setOrderDate(order.getOrderDate());
        dto.setStatus(order.getStatus());
        dto.setTrackingNumber(order.getTrackingNumber());
        dto.setDestinationX(order.getDestinationX());
        dto.setDestinationY(order.getDestinationY());
        return dto;
    }
}