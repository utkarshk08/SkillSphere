package com.skillsphere.repository;

import com.skillsphere.domain.Community;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository boundary for searchable community records.
 */
public interface CommunityRepository extends JpaRepository<Community, Long> {

    Page<Community> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String name,
            String description,
            Pageable pageable
    );

    boolean existsByNameIgnoreCase(String name);

    List<Community> findByMembersId(Long userId);
}
