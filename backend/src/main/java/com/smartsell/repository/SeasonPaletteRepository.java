package com.smartsell.repository;

import com.smartsell.entity.SeasonPalette;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeasonPaletteRepository extends JpaRepository<SeasonPalette, Long> {
    List<SeasonPalette> findBySeasonOrderBySortOrder(String season);
}
