package com.skillmatrix.domain.repository;

import com.skillmatrix.domain.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByTeamNameIgnoreCase(String teamName);

    boolean existsByTeamNameIgnoreCase(String teamName);

    List<Team> findAllByTeamNameIn(Collection<String> teamNames);
}
