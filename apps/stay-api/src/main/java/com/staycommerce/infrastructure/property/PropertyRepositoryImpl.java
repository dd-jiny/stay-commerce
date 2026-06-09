package com.staycommerce.infrastructure.property;

import com.staycommerce.domain.property.Property;
import com.staycommerce.domain.property.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class PropertyRepositoryImpl implements PropertyRepository {

    private final PropertyJpaRepository propertyJpaRepository;

    @Override
    public Property save(Property property) {
        return propertyJpaRepository.save(property);
    }

    @Override
    public Optional<Property> findById(Long id) {
        return propertyJpaRepository.findById(id);
    }

    @Override
    public List<Property> findByCity(String city) {
        return propertyJpaRepository.findByCity(city);
    }
}
