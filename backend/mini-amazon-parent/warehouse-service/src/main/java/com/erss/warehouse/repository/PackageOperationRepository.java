package com.erss.warehouse.repository;


import com.erss.warehouse.entity.PackageOperation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PackageOperationRepository extends JpaRepository<PackageOperation, Long> {
    List<PackageOperation> findByPackageId(Long packageId);
    Optional<PackageOperation> findByPackageIdAndOperation(Long packageId, String operation);
    List<PackageOperation> findByStatus(String status);

    List<PackageOperation> findByStatusAndOperation(String pending, String purchase);

    List<PackageOperation> findByUserIdAndOperation(Integer userId, String operation);



//    List<PackageOperation> findByUserIdOrderByIdDesc(Integer userId);
//
//
//    List<PackageOperation> findByOperationAndUserIdOrderByIdDesc(String operation, Integer userId);
//
//
//    Optional<PackageOperation> findFirstByUserIdAndOperationOrderByIdDesc(Integer userId, String operation);

}
