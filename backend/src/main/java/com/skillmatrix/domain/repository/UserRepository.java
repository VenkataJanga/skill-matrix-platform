package com.skillmatrix.domain.repository;

import com.skillmatrix.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByPublicId(String publicId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /** Load user with all active roles eagerly to avoid N+1 on login. */
    @Query("""
            SELECT DISTINCT u FROM User u
            LEFT JOIN FETCH u.userRoles ur
            LEFT JOIN FETCH ur.role r
            LEFT JOIN FETCH r.rolePermissions rp
            LEFT JOIN FETCH rp.permission
            WHERE u.username = :username
            """)
    Optional<User> findByUsernameWithRolesAndPermissions(@Param("username") String username);

    /**
     * Paginated search across username, fullName, email and employeeId.
     * Optionally filter by roleCode and/or active status.
     */
    @Query("""
            SELECT DISTINCT u FROM User u
            LEFT JOIN u.userRoles ur
            LEFT JOIN ur.role r
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%',:search,'%'))
                   OR LOWER(u.fullName)  LIKE LOWER(CONCAT('%',:search,'%'))
                   OR LOWER(u.email)     LIKE LOWER(CONCAT('%',:search,'%'))
                   OR LOWER(u.employeeId) LIKE LOWER(CONCAT('%',:search,'%')))
              AND (:roleCode IS NULL OR :roleCode = ''
                   OR (ur.active = true AND r.roleCode = :roleCode))
              AND (:activeFilter IS NULL OR u.active = :activeFilter)
            """)
    Page<User> searchUsers(
            @Param("search") String search,
            @Param("roleCode") String roleCode,
            @Param("activeFilter") Boolean activeFilter,
            Pageable pageable);
}
