package com.example.apimock.repository;

import com.example.apimock.entity.FieldConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FieldConfigRepository extends JpaRepository<FieldConfig, Long> {
}
