package com.erss.warehouse.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "package_operations")
public class PackageOperation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long packageId;

    @Column(nullable = false)
    private String operation; // PACK, LOAD

    @Column(nullable = false)
    private String status; // PENDING, COMPLETED, FAILED

    @Column
    private String orderItems;

    private Integer truckId;

    private Integer userId;

    private Integer destinationX;
    private Integer destinationY;








}



