package com.staycommerce.domain.property;

import java.util.List;
import java.util.Optional;

public interface PropertyRepository {
    Property save(Property property);
    Optional<Property> findById(Long id);
    List<Property> findByCity(String city);
}
