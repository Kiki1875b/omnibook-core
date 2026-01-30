package com.sprint.omnibook.broker.domain.repository;

import com.sprint.omnibook.broker.domain.Property;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyRepository extends JpaRepository<Property, Long> {
}
