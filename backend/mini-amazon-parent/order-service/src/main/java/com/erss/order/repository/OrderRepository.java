package com.erss.order.repository;

import com.erss.order.entity.Order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
//    List<Order> findByUpsAccount(String upsAccount);
    List<Order> findByUpsAccount(String upsAccount);
}
