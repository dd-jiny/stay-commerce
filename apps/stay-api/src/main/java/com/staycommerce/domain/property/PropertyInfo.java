package com.staycommerce.domain.property;

public record PropertyInfo(
    Long id,
    String name,
    String city,
    String address,
    String description,
    PropertyType type
) {
    public static PropertyInfo from(Property property) {
        return new PropertyInfo(
            property.getId(),
            property.getName(),
            property.getCity(),
            property.getAddress(),
            property.getDescription(),
            property.getType()
        );
    }
}
