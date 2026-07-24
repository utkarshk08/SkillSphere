package com.skillsphere.repository;

import com.skillsphere.domain.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Database access for project lists, ownership views, and community views.
 */
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Page<Project> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String title,
            String description,
            Pageable pageable
    );

    Page<Project> findByOwnerId(Long ownerId, Pageable pageable);

    List<Project> findAllByOwnerId(Long ownerId);

    List<Project> findByMembersId(Long userId);

    Page<Project> findByCommunityId(Long communityId, Pageable pageable);

    List<Project> findByCommunityId(Long communityId);

    long countByOwnerId(Long ownerId);
}
