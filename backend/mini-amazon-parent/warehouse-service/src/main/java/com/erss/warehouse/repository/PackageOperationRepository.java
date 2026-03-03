package com.erss.warehouse.repository;


import com.erss.warehouse.entity.PackageOperation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PackageOperationRepository extends JpaRepository<PackageOperation, Long> {
    List<PackageOperation> findByPackageId(Long packageId);
    Optional<PackageOperation> findByPackageIdAndOperation(Long packageId, String operation);
    List<PackageOperation> findByStatus(String status);

    List<PackageOperation> findByStatusAndOperation(String pending, String purchase);

    List<PackageOperation> findByUserIdAndOperation(Integer userId, String operation);

    /**
     * Find and lock the first ready package for loading
     * Uses pessimistic write lock (SELECT FOR UPDATE) to prevent concurrent claims
     *
     * @return Optional containing the locked PackageOperation, or empty if no ready packages
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PackageOperation p " +
           "WHERE p.status = 'COMPLETED' AND p.operation = 'PACK' " +
           "ORDER BY p.id ASC")
    List<PackageOperation> findAndLockFirstReadyPackage();

//    List<PackageOperation> findByUserIdOrderByIdDesc(Integer userId);
//
//
//    List<PackageOperation> findByOperationAndUserIdOrderByIdDesc(String operation, Integer userId);
//
//
//    Optional<PackageOperation> findFirstByUserIdAndOperationOrderByIdDesc(Integer userId, String operation);

}
