package com.skillsphere.repository;

import com.skillsphere.domain.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    Page<Announcement> findByActiveTrue(Pageable pageable);
    long deleteByCreatedById(Long createdById);
}
