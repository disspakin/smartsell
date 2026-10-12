package com.smartsell.repository;

import com.smartsell.entity.SizeChartRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SizeChartRowRepository extends JpaRepository<SizeChartRow, Long> {
    // Spring Data สร้าง query จากชื่อเมธอดให้เอง:
    // WHERE size_chart_id = ? ORDER BY sort_order
    List<SizeChartRow> findBySizeChartIdOrderBySortOrder(Long sizeChartId);
}
