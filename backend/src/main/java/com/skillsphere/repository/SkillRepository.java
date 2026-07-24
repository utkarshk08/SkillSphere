package com.skillsphere.repository;

import com.skillsphere.domain.Skill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data repository for personal skill entries. Method-name queries keep the
 * beginner-friendly search logic close to the data boundary without handwritten SQL.
 */
public interface SkillRepository extends JpaRepository<Skill, Long> {

    Page<Skill> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String name,
            String description,
            Pageable pageable
    );

    Page<Skill> findByUserId(Long userId, Pageable pageable);

    List<Skill> findAllByUserId(Long userId);

    long deleteByUserId(Long userId);
}
