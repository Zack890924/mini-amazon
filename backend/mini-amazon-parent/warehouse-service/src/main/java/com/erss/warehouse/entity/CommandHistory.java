package com.erss.warehouse.entity;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "command_history",
        uniqueConstraints = @UniqueConstraint(columnNames = {"package_id", "truck_id", "command_type"}))
@Data
public class CommandHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long seqNum;

    @Column(name = "package_id")
    private Long packageId;

    @Column(name = "truck_id")
    private Integer truckId;

    @Column(name = "command_type")
    private String commandType;


}
