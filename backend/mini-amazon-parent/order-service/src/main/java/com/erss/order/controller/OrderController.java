package com.erss.order.controller;

import com.erss.order.dto.OrderDTO;
import com.erss.order.dto.PurchaseRequestDTO;
import com.erss.order.dto.PurchaseResponseDTO;
import com.erss.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<PurchaseResponseDTO> purchase(@Valid @RequestBody PurchaseRequestDTO req){

        return ResponseEntity.ok(orderService.placeOrder(req));
    }

    @GetMapping("/account/{upsAccount}")
    public ResponseEntity<List<OrderDTO>> getOrdersByUpsAccount(@PathVariable String upsAccount) {
        List<OrderDTO> orders = orderService.findOrdersByUpsAccount(upsAccount);
        return ResponseEntity.ok(orders);
    }



}
