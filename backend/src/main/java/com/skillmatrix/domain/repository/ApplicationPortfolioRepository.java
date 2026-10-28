package com.skillmatrix.domain.repository;

import com.skillmatrix.domain.entity.ApplicationPortfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationPortfolioRepository extends JpaRepository<ApplicationPortfolio, Long> {

    Optional<ApplicationPortfolio> findByPortfolioCode(String portfolioCode);

    List<ApplicationPortfolio> findAllByActiveTrue();

    /**
     * Validates that a portfolio code belongs to the given Application Type + Bundle
     * hierarchy combination — used by Application Master create/update/import to
     * enforce hierarchy consistency (Problem Statement Part A requirement #4).
     */
    @Query("""
            SELECT p FROM ApplicationPortfolio p
            JOIN p.applicationType at
            JOIN p.bundle b
            WHERE p.portfolioCode = :portfolioCode
              AND at.typeCode = :typeCode
              AND b.bundleCode = :bundleCode
            """)
    Optional<ApplicationPortfolio> findByPortfolioCodeAndTypeCodeAndBundleCode(
            @Param("portfolioCode") String portfolioCode,
            @Param("typeCode") String typeCode,
            @Param("bundleCode") String bundleCode);
}
