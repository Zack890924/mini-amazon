package com.erss.warehouse.repository;

import com.erss.warehouse.entity.TrackingInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TrackingInfoRepository extends JpaRepository<TrackingInfo, Long> {
    Optional<TrackingInfo> findByPackageId(Long packageId);
    Optional<TrackingInfo> findByTrackingNumber(String trackingNumber);
}
