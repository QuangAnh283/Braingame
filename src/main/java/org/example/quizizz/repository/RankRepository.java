package org.example.quizizz.repository;

import org.example.quizizz.model.entity.Rank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RankRepository extends JpaRepository<Rank, Long> {
    Optional<Rank> findByUserId(Long userId);
    long countByTotalScoreGreaterThan(Integer totalScore);
}
