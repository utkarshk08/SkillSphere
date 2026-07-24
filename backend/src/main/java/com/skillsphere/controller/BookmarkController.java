package com.skillsphere.controller;

import com.skillsphere.domain.User;
import com.skillsphere.dto.bookmark.BookmarkRequest;
import com.skillsphere.dto.bookmark.BookmarkResponse;
import com.skillsphere.dto.common.MessageResponse;
import com.skillsphere.service.bookmark.BookmarkService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Full CRUD endpoints for a student's profile and community bookmarks. */
@RestController
@RequestMapping("/api/bookmarks")
@Tag(name = "Bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @GetMapping
    public Page<BookmarkResponse> getMine(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        return bookmarkService.getMine(currentUser, pageable);
    }

    @GetMapping("/{bookmarkId}")
    public BookmarkResponse getById(@PathVariable Long bookmarkId, @AuthenticationPrincipal User currentUser) {
        return bookmarkService.getById(bookmarkId, currentUser);
    }

    @PostMapping
    public BookmarkResponse create(@Valid @RequestBody BookmarkRequest request, @AuthenticationPrincipal User currentUser) {
        return bookmarkService.create(request, currentUser);
    }

    @PutMapping("/{bookmarkId}")
    public BookmarkResponse update(
            @PathVariable Long bookmarkId,
            @Valid @RequestBody BookmarkRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return bookmarkService.update(bookmarkId, request, currentUser);
    }

    @DeleteMapping("/{bookmarkId}")
    public MessageResponse delete(@PathVariable Long bookmarkId, @AuthenticationPrincipal User currentUser) {
        bookmarkService.delete(bookmarkId, currentUser);
        return new MessageResponse("Bookmark deleted successfully.");
    }
}
