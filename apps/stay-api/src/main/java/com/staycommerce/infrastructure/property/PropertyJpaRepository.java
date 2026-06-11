package com.staycommerce.infrastructure.property;

import com.staycommerce.domain.property.Property;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropertyJpaRepository extends JpaRepository<Property, Long> {
    List<Property> findByCity(String city);
}
