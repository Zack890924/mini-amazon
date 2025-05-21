package com.erss.warehouse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    @Column(name = "world_product_id", unique = true, nullable = false)
    @Column(unique = true, nullable = false)
    private Long worldProductId;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Integer inventory = 0;
}
