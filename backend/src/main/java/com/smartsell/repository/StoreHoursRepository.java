package com.smartsell.repository;

import com.smartsell.entity.StoreHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreHoursRepository extends JpaRepository<StoreHours, Long> {
    List<StoreHours> findAllByOrderByDayOfWeekAsc();
}
