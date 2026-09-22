package com.tuckersoft.branchengine.repository;

import com.tuckersoft.branchengine.entity.Decision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

// JpaSpecificationExecutor habilita los filtros combinables de GET /api/v1/decisions
// (branchType, impactLevel, status, playthroughId) con paginacion 0-based.
public interface DecisionRepository
        extends JpaRepository<Decision, Long>, JpaSpecificationExecutor<Decision> {

    // /path : solo las que movieron la historia, createdAt ASC
    List<Decision> findByPlaythroughIdAndResolvedNodeCodeIsNotNullOrderByCreatedAtAsc(Long playthroughId);
}
