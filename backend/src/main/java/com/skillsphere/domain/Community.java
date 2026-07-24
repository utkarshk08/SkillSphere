package com.skillsphere.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A topic-based group that students can join to discover resources and projects.
 *
 * Members use a {@link ManyToMany} relation because one student can join many
 * communities and each community can contain many students. Resources are just URL
 * strings in an element collection; that keeps the requested feature useful without
 * adding an unnecessary resource-management module or file storage layer.
 */
@Entity
@Table(name = "communities")
public class Community {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(nullable = false, length = 3000)
    private String description;

    @ElementCollection
    @CollectionTable(name = "community_resources", joinColumns = @JoinColumn(name = "community_id"))
    @Column(name = "resource_url", length = 500)
    private Set<String> resources = new LinkedHashSet<>();

    @ManyToMany
    @JoinTable(
            name = "community_members",
            joinColumns = @JoinColumn(name = "community_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> members = new LinkedHashSet<>();

    /** The inverse side: a project stores the actual foreign key to its community. */
    @OneToMany(mappedBy = "community", fetch = FetchType.LAZY)
    private Set<Project> projects = new LinkedHashSet<>();

    public Community() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getResources() {
        return resources;
    }

    public void setResources(Set<String> resources) {
        this.resources = resources == null ? new LinkedHashSet<>() : new LinkedHashSet<>(resources);
    }

    public Set<User> getMembers() {
        return members;
    }

    public void setMembers(Set<User> members) {
        this.members = members == null ? new LinkedHashSet<>() : new LinkedHashSet<>(members);
    }

    public Set<Project> getProjects() {
        return projects;
    }
}
