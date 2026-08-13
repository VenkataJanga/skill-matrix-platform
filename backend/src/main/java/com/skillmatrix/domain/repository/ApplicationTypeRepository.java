package com.skillmatrix.domain.repository;

import com.skillmatrix.domain.entity.ApplicationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationTypeRepository extends JpaRepository<ApplicationType, Long> {

    List<ApplicationType> findAllByActiveTrue();

    Optional<ApplicationType> findByTypeCode(String typeCode);

    Optional<ApplicationType> findByPublicId(String publicId);
}
