package com.skillsphere.service.bookmark;

import com.skillsphere.domain.Bookmark;
import com.skillsphere.domain.BookmarkTargetType;
import com.skillsphere.domain.Community;
import com.skillsphere.domain.Role;
import com.skillsphere.domain.User;
import com.skillsphere.dto.bookmark.BookmarkRequest;
import com.skillsphere.dto.bookmark.BookmarkResponse;
import com.skillsphere.exception.BadRequestException;
import com.skillsphere.exception.ResourceNotFoundException;
import com.skillsphere.exception.UnauthorizedException;
import com.skillsphere.repository.BookmarkRepository;
import com.skillsphere.repository.CommunityRepository;
import com.skillsphere.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Supports the requested profile and community bookmarks in one simple entity. The
 * service enforces that exactly one target relation is set for every saved bookmark.
 */
@Service
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final CommunityRepository communityRepository;

    public BookmarkService(
            BookmarkRepository bookmarkRepository,
            UserRepository userRepository,
            CommunityRepository communityRepository
    ) {
        this.bookmarkRepository = bookmarkRepository;
        this.userRepository = userRepository;
        this.communityRepository = communityRepository;
    }

    @Transactional(readOnly = true)
    public Page<BookmarkResponse> getMine(User currentUser, Pageable pageable) {
        return bookmarkRepository.findByUserId(currentUser.getId(), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public BookmarkResponse getById(Long bookmarkId, User currentUser) {
        Bookmark bookmark = findBookmark(bookmarkId);
        requireOwnerOrAdmin(bookmark, currentUser);
        return toResponse(bookmark);
    }

    @Transactional
    public BookmarkResponse create(BookmarkRequest request, User currentUser) {
        ensureNotDuplicate(request, currentUser);
        Bookmark bookmark = new Bookmark();
        bookmark.setUser(currentUser);
        applyTarget(bookmark, request, currentUser);
        return toResponse(bookmarkRepository.save(bookmark));
    }

    @Transactional
    public BookmarkResponse update(Long bookmarkId, BookmarkRequest request, User currentUser) {
        Bookmark bookmark = findBookmark(bookmarkId);
        requireOwnerOrAdmin(bookmark, currentUser);
        ensureNotDuplicateExceptSelf(request, currentUser, bookmarkId);
        applyTarget(bookmark, request, currentUser);
        return toResponse(bookmarkRepository.save(bookmark));
    }

    @Transactional
    public void delete(Long bookmarkId, User currentUser) {
        Bookmark bookmark = findBookmark(bookmarkId);
        requireOwnerOrAdmin(bookmark, currentUser);
        bookmarkRepository.delete(bookmark);
    }

    private void applyTarget(Bookmark bookmark, BookmarkRequest request, User currentUser) {
        bookmark.setTargetType(request.targetType());
        bookmark.setTargetUser(null);
        bookmark.setTargetCommunity(null);
        if (request.targetType() == BookmarkTargetType.PROFILE) {
            if (currentUser.getId().equals(request.targetId())) {
                throw new BadRequestException("You cannot bookmark your own profile.");
            }
            bookmark.setTargetUser(userRepository.findById(request.targetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Profile not found: " + request.targetId())));
        } else if (request.targetType() == BookmarkTargetType.COMMUNITY) {
            bookmark.setTargetCommunity(communityRepository.findById(request.targetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + request.targetId())));
        } else {
            throw new BadRequestException("Unsupported bookmark target.");
        }
    }

    private void ensureNotDuplicate(BookmarkRequest request, User user) {
        boolean exists = request.targetType() == BookmarkTargetType.PROFILE
                ? bookmarkRepository.findByUserIdAndTargetUserId(user.getId(), request.targetId()).isPresent()
                : bookmarkRepository.findByUserIdAndTargetCommunityId(user.getId(), request.targetId()).isPresent();
        if (exists) {
            throw new BadRequestException("This item is already bookmarked.");
        }
    }

    private void ensureNotDuplicateExceptSelf(BookmarkRequest request, User user, Long bookmarkId) {
        Bookmark existing = request.targetType() == BookmarkTargetType.PROFILE
                ? bookmarkRepository.findByUserIdAndTargetUserId(user.getId(), request.targetId()).orElse(null)
                : bookmarkRepository.findByUserIdAndTargetCommunityId(user.getId(), request.targetId()).orElse(null);
        if (existing != null && !existing.getId().equals(bookmarkId)) {
            throw new BadRequestException("This item is already bookmarked.");
        }
    }

    private Bookmark findBookmark(Long bookmarkId) {
        return bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new ResourceNotFoundException("Bookmark not found: " + bookmarkId));
    }

    private void requireOwnerOrAdmin(Bookmark bookmark, User user) {
        if (!bookmark.getUser().getId().equals(user.getId()) && user.getRole() != Role.ROLE_ADMIN) {
            throw new UnauthorizedException("You cannot manage this bookmark.");
        }
    }

    private BookmarkResponse toResponse(Bookmark bookmark) {
        if (bookmark.getTargetType() == BookmarkTargetType.PROFILE) {
            User user = bookmark.getTargetUser();
            return new BookmarkResponse(bookmark.getId(), bookmark.getTargetType(), user.getId(), user.getFullName(),
                    user.getProfilePicturePath(), bookmark.getCreatedAt());
        }
        Community community = bookmark.getTargetCommunity();
        return new BookmarkResponse(bookmark.getId(), bookmark.getTargetType(), community.getId(), community.getName(),
                null, bookmark.getCreatedAt());
    }
}
