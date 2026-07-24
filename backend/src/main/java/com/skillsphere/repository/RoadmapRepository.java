package com.skillsphere.repository;

import com.skillsphere.domain.Roadmap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoadmapRepository extends JpaRepository<Roadmap, Long> {
    Page<Roadmap> findByPublicVisibleTrue(Pageable pageable);
    Page<Roadmap> findByOwnerId(Long ownerId, Pageable pageable);
    List<Roadmap> findAllByOwnerId(Long ownerId);
}
