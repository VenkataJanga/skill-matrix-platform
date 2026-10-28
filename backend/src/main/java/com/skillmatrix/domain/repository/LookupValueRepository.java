package com.skillmatrix.domain.repository;

import com.skillmatrix.domain.entity.LookupValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LookupValueRepository extends JpaRepository<LookupValue, Long> {

    @Query("""
            SELECT lv FROM LookupValue lv
            JOIN lv.lookupType lt
            WHERE lt.typeCode = :typeCode AND lv.active = true
            ORDER BY lv.displayOrder
            """)
    List<LookupValue> findActiveByTypeCode(@Param("typeCode") String typeCode);

    @Query("""
            SELECT lv FROM LookupValue lv
            JOIN lv.lookupType lt
            WHERE lt.typeCode = :typeCode AND lv.valueCode = :valueCode AND lv.active = true
            """)
    Optional<LookupValue> findByTypeCodeAndValueCode(
            @Param("typeCode")  String typeCode,
            @Param("valueCode") String valueCode);
}
