package com.smartsell.repository;

import com.smartsell.entity.CustomerSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerSessionRepository extends JpaRepository<CustomerSession, Long> {
}
