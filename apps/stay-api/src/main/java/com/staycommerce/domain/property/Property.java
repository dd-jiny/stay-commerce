package com.staycommerce.domain.property;

import com.staycommerce.domain.BaseEntity;
import com.staycommerce.support.error.CoreException;
import com.staycommerce.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "property")
public class Property extends BaseEntity {

    private static final int NAME_MAX = 200;
    private static final int CITY_MAX = 50;
    private static final int ADDRESS_MAX = 500;

    @Column(nullable = false, length = NAME_MAX)
    private String name;

    @Column(nullable = false, length = CITY_MAX)
    private String city;

    @Column(nullable = false, length = ADDRESS_MAX)
    private String address;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PropertyType type;

    protected Property() {}

    public Property(String name, String city, String address, String description, PropertyType type) {
        validateName(name);
        validateCity(city);
        validateAddress(address);
        validateType(type);

        this.name = name;
        this.city = city;
        this.address = address;
        this.description = description;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public String getAddress() {
        return address;
    }

    public String getDescription() {
        return description;
    }

    public PropertyType getType() {
        return type;
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "숙소명은 비어있을 수 없습니다.");
        }
        if (name.length() > NAME_MAX) {
            throw new CoreException(ErrorType.BAD_REQUEST, "숙소명은 200자를 초과할 수 없습니다.");
        }
    }

    private void validateCity(String city) {
        if (city == null || city.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "도시는 비어있을 수 없습니다.");
        }
        if (city.length() > CITY_MAX) {
            throw new CoreException(ErrorType.BAD_REQUEST, "도시는 50자를 초과할 수 없습니다.");
        }
    }

    private void validateAddress(String address) {
        if (address == null || address.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주소는 비어있을 수 없습니다.");
        }
        if (address.length() > ADDRESS_MAX) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주소는 500자를 초과할 수 없습니다.");
        }
    }

    private void validateType(PropertyType type) {
        if (type == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "숙소 유형은 비어있을 수 없습니다.");
        }
    }
}
