package com.skillmatrix.domain.repository;

import com.skillmatrix.domain.entity.Bundle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BundleRepository extends JpaRepository<Bundle, Long> {

    List<Bundle> findAllByActiveTrue();

    Optional<Bundle> findByBundleCode(String bundleCode);

    Optional<Bundle> findByPublicId(String publicId);
}
