package com.tuckersoft.branchengine.repository;

import com.tuckersoft.branchengine.entity.Playthrough;
import com.tuckersoft.branchengine.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaythroughRepository extends JpaRepository<Playthrough, Long> {
    boolean existsByPlayerTag(String playerTag);

    // GET /api/v1/playthroughs -> ROLE_USER: solo las suyas, createdAt DESC
    List<Playthrough> findByUserOrderByCreatedAtDesc(User user);

    // GET /api/v1/playthroughs -> ROLE_ADMIN: todas, createdAt DESC
    List<Playthrough> findAllByOrderByCreatedAtDesc();
}
