package com.staycommerce.infrastructure.example;

import com.staycommerce.domain.example.ExampleModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExampleJpaRepository extends JpaRepository<ExampleModel, Long> {}
