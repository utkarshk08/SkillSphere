package com.skillsphere.service.roadmap;

import com.skillsphere.domain.Roadmap;
import com.skillsphere.domain.RoadmapItem;
import com.skillsphere.domain.Role;
import com.skillsphere.domain.User;
import com.skillsphere.dto.roadmap.RoadmapItemRequest;
import com.skillsphere.dto.roadmap.RoadmapItemResponse;
import com.skillsphere.dto.roadmap.RoadmapRequest;
import com.skillsphere.dto.roadmap.RoadmapResponse;
import com.skillsphere.exception.ResourceNotFoundException;
import com.skillsphere.exception.UnauthorizedException;
import com.skillsphere.repository.RoadmapRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Owns roadmap CRUD and calculates progress from completed roadmap items.
 * Calculating instead of saving a percentage avoids duplicated data and keeps the logic
 * easy to explain in an interview.
 */
@Service
public class RoadmapService {

    private final RoadmapRepository roadmapRepository;

    public RoadmapService(RoadmapRepository roadmapRepository) {
        this.roadmapRepository = roadmapRepository;
    }

    @Transactional(readOnly = true)
    public Page<RoadmapResponse> getPublicRoadmaps(Pageable pageable) {
        return roadmapRepository.findByPublicVisibleTrue(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<RoadmapResponse> getMyRoadmaps(User currentUser, Pageable pageable) {
        return roadmapRepository.findByOwnerId(currentUser.getId(), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public RoadmapResponse getById(Long roadmapId, User currentUser) {
        Roadmap roadmap = findRoadmap(roadmapId);
        if (!roadmap.isPublicVisible() && !isOwnerOrAdmin(roadmap, currentUser)) {
            throw new UnauthorizedException("You cannot view this private roadmap.");
        }
        return toResponse(roadmap);
    }

    @Transactional
    public RoadmapResponse create(RoadmapRequest request, User currentUser) {
        Roadmap roadmap = new Roadmap();
        roadmap.setOwner(currentUser);
        applyRequest(roadmap, request);
        return toResponse(roadmapRepository.save(roadmap));
    }

    @Transactional
    public RoadmapResponse update(Long roadmapId, RoadmapRequest request, User currentUser) {
        Roadmap roadmap = findRoadmap(roadmapId);
        requireOwnerOrAdmin(roadmap, currentUser);
        applyRequest(roadmap, request);
        return toResponse(roadmapRepository.save(roadmap));
    }

    @Transactional
    public void delete(Long roadmapId, User currentUser) {
        Roadmap roadmap = findRoadmap(roadmapId);
        requireOwnerOrAdmin(roadmap, currentUser);
        roadmapRepository.delete(roadmap);
    }

    private Roadmap findRoadmap(Long roadmapId) {
        return roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new ResourceNotFoundException("Roadmap not found: " + roadmapId));
    }

    private void applyRequest(Roadmap roadmap, RoadmapRequest request) {
        roadmap.setTitle(request.title().trim());
        roadmap.setPublicVisible(request.publicVisible());
        List<RoadmapItem> items = request.items() == null ? List.of() : request.items().stream()
                .map(item -> toEntity(item))
                .toList();
        roadmap.replaceItems(items);
    }

    private RoadmapItem toEntity(RoadmapItemRequest request) {
        return new RoadmapItem(request.title().trim(), request.completed());
    }

    private void requireOwnerOrAdmin(Roadmap roadmap, User user) {
        if (!isOwnerOrAdmin(roadmap, user)) {
            throw new UnauthorizedException("You can only manage your own roadmap.");
        }
    }

    private boolean isOwnerOrAdmin(Roadmap roadmap, User user) {
        return user != null && (roadmap.getOwner().getId().equals(user.getId()) || user.getRole() == Role.ROLE_ADMIN);
    }

    private RoadmapResponse toResponse(Roadmap roadmap) {
        List<RoadmapItemResponse> items = roadmap.getItems().stream()
                .map(item -> new RoadmapItemResponse(item.getId(), item.getTitle(), item.isCompleted()))
                .toList();
        int completeCount = (int) items.stream().filter(RoadmapItemResponse::completed).count();
        int progress = items.isEmpty() ? 0 : Math.round((completeCount * 100f) / items.size());
        return new RoadmapResponse(
                roadmap.getId(),
                roadmap.getOwner().getId(),
                roadmap.getOwner().getUsername(),
                roadmap.getTitle(),
                roadmap.isPublicVisible(),
                items,
                progress,
                roadmap.getCreatedAt()
        );
    }
}
