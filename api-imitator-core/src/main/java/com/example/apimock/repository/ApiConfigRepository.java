package com.example.apimock.repository;

import com.example.apimock.entity.ApiConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiConfigRepository extends JpaRepository<ApiConfig, Long> {

    @Query("SELECT a FROM ApiConfig a LEFT JOIN FETCH a.fields f LEFT JOIN FETCH f.children WHERE a.path = :path AND a.method = :method")
    Optional<ApiConfig> findByPathAndMethodWithFields(@Param("path") String path, @Param("method") String method);
}
