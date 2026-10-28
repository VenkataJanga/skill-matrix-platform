package com.skillmatrix.domain.repository;

import com.skillmatrix.domain.entity.SkillCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillCategoryRepository extends JpaRepository<SkillCategory, Long> {

    List<SkillCategory> findAllByActiveTrueOrderByDisplayOrderAsc();

    Optional<SkillCategory> findByCategoryCode(String categoryCode);

    Optional<SkillCategory> findByPublicId(String publicId);

    boolean existsByCategoryCode(String categoryCode);
}
