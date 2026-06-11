package com.staycommerce.domain.property;

import com.staycommerce.support.error.CoreException;
import com.staycommerce.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Component
public class PropertyService {

    private final PropertyRepository propertyRepository;

    @Transactional
    public PropertyInfo register(String name, String city, String address,
                                 String description, PropertyType type) {
        Property property = new Property(name, city, address, description, type);
        return PropertyInfo.from(propertyRepository.save(property));
    }

    @Transactional(readOnly = true)
    public PropertyInfo getById(Long id) {
        Property property = propertyRepository.findById(id)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 숙소입니다."));
        return PropertyInfo.from(property);
    }

    @Transactional(readOnly = true)
    public List<PropertyInfo> findByCity(String city) {
        return propertyRepository.findByCity(city).stream()
            .map(PropertyInfo::from)
            .toList();
    }
}
