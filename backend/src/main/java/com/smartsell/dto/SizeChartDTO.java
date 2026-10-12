package com.smartsell.dto;

import java.math.BigDecimal;
import java.util.List;

// what GET /api/products/{id}/size-chart returns — rows are already in sort_order
public record SizeChartDTO(String name, String unit, boolean isEstimated, List<Row> rows) {

    public record Row(String size, BigDecimal chestIn, BigDecimal lengthIn) {}
}
