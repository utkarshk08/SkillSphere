package com.skillsphere.repository;

import com.skillsphere.domain.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    Page<Bookmark> findByUserId(Long userId, Pageable pageable);
    Optional<Bookmark> findByUserIdAndTargetUserId(Long userId, Long targetUserId);
    Optional<Bookmark> findByUserIdAndTargetCommunityId(Long userId, Long targetCommunityId);
    long deleteByUserIdOrTargetUserId(Long userId, Long targetUserId);
    long deleteByTargetCommunityId(Long communityId);
}
