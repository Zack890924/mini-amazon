package com.erss.warehouse.repository;


import com.erss.warehouse.entity.CommandHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommandHistoryRepository extends JpaRepository<CommandHistory, Long> {
    boolean existsBySeqNum(Long seqNum);
    boolean existsByPackageIdAndCommandType(Long packageId, String commandType);
    boolean existsByPackageIdAndTruckIdAndCommandType(Long packageId, Integer truckId, String commandType);
}
