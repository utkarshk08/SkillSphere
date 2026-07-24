package com.skillsphere.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A student-created collaboration project.
 *
 * The project owns simple string collections for images, technologies, and required
 * skills. That is more suitable here than creating database entities for every tag.
 * The owner is also automatically added to members by ProjectService, which makes
 * current-member and open-position values intuitive. Those two values are calculated
 * with {@link Transient} getters so they can never become stale database columns.
 */
@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 3000)
    private String description;

    @Column(length = 500)
    private String githubLink;

    @ElementCollection
    @CollectionTable(name = "project_images", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "image_path", length = 500)
    private Set<String> projectImages = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "project_tech_stack", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "technology", length = 100)
    private Set<String> techStack = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "project_required_skills", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "required_skill", length = 100)
    private Set<String> requiredSkills = new LinkedHashSet<>();

    @Column(nullable = false)
    private LocalDate deadline;

    @Column(nullable = false)
    private Integer maximumMembers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DifficultyLevel difficultyLevel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToMany
    @JoinTable(
            name = "project_members",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> members = new LinkedHashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    public Project() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGithubLink() {
        return githubLink;
    }

    public void setGithubLink(String githubLink) {
        this.githubLink = githubLink;
    }

    public Set<String> getProjectImages() {
        return projectImages;
    }

    public void setProjectImages(Set<String> projectImages) {
        this.projectImages = projectImages == null ? new LinkedHashSet<>() : new LinkedHashSet<>(projectImages);
    }

    public Set<String> getTechStack() {
        return techStack;
    }

    public void setTechStack(Set<String> techStack) {
        this.techStack = techStack == null ? new LinkedHashSet<>() : new LinkedHashSet<>(techStack);
    }

    public Set<String> getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(Set<String> requiredSkills) {
        this.requiredSkills = requiredSkills == null ? new LinkedHashSet<>() : new LinkedHashSet<>(requiredSkills);
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public Integer getMaximumMembers() {
        return maximumMembers;
    }

    public void setMaximumMembers(Integer maximumMembers) {
        this.maximumMembers = maximumMembers;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public DifficultyLevel getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(DifficultyLevel difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public Set<User> getMembers() {
        return members;
    }

    public void setMembers(Set<User> members) {
        this.members = members == null ? new LinkedHashSet<>() : new LinkedHashSet<>(members);
    }

    public Community getCommunity() {
        return community;
    }

    public void setCommunity(Community community) {
        this.community = community;
    }

    /**
     * This is derived from the member relation rather than persisted, so it stays
     * correct after every add or remove operation.
     */
    @Transient
    public int getCurrentMemberCount() {
        return members.size();
    }

    /**
     * Never returns a negative value, even if old data is manually edited.
     */
    @Transient
    public int getOpenPositions() {
        if (maximumMembers == null) {
            return 0;
        }
        return Math.max(0, maximumMembers - getCurrentMemberCount());
    }
}
