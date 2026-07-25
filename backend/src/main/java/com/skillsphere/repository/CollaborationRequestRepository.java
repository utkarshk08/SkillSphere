package com.skillsphere.repository;

import com.skillsphere.domain.CollaborationRequest;
import com.skillsphere.domain.CollaborationRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CollaborationRequestRepository extends JpaRepository<CollaborationRequest, Long> {

    boolean existsBySenderIdAndReceiverIdAndStatusAndProjectIsNull(
            Long senderId,
            Long receiverId,
            CollaborationRequestStatus status
    );

    boolean existsBySenderIdAndProjectIdAndStatus(
            Long senderId,
            Long projectId,
            CollaborationRequestStatus status
    );

    long deleteBySenderIdOrReceiverId(Long senderId, Long receiverId);

    long deleteByProjectId(Long projectId);

    @Query("""
            select request from CollaborationRequest request
            left join request.project project
            where (request.sender.id = :userId or request.receiver.id = :userId)
              and (:search is null or lower(request.message) like lower(concat('%', :search, '%'))
                or lower(request.responseMessage) like lower(concat('%', :search, '%'))
                or lower(request.sender.username) like lower(concat('%', :search, '%'))
                or lower(request.receiver.username) like lower(concat('%', :search, '%'))
                or lower(project.title) like lower(concat('%', :search, '%')))
            """)
    Page<CollaborationRequest> findForUser(
            @Param("userId") Long userId,
            @Param("search") String search,
            Pageable pageable
    );
}
