package com.smartsell.repository;

import com.smartsell.entity.StoreInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreInfoRepository extends JpaRepository<StoreInfo, Long> {
    // เรียงตาม id = ลำดับที่ร้านใส่ไว้
    List<StoreInfo> findAllByOrderByIdAsc();
}
