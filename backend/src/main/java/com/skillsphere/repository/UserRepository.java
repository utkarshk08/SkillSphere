package com.skillsphere.repository;

import com.skillsphere.domain.Role;
import com.skillsphere.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Data-access boundary for application accounts.
 *
 * Spring Data JPA generates these simple queries, keeping SQL out of controllers and
 * services while preserving the requested Controller -> Service -> Repository layering.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByRole(Role role);

    Page<User> findByPublicProfileVisibilityTrue(Pageable pageable);

    Page<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrCollegeNameContainingIgnoreCase(
            String username,
            String email,
            String collegeName,
            Pageable pageable
    );

    /**
     * One focused JPQL query supports the profile search fields named in the project.
     * Optional filters can be combined without adding a Specification framework.
     */
    @Query("""
            select distinct user from User user
            left join Skill skill on skill.user = user
            left join user.interests interest
            where user.publicProfileVisibility = true
              and user.role = :role
              and (:name is null or lower(user.username) like lower(concat('%', :name, '%'))
                   or lower(concat(coalesce(user.firstName, ''), ' ', coalesce(user.lastName, ''))) like lower(concat('%', :name, '%')))
              and (:college is null or lower(user.collegeName) like lower(concat('%', :college, '%')))
              and (:country is null or lower(user.country) like lower(concat('%', :country, '%')))
              and (:skillName is null or lower(skill.name) like lower(concat('%', :skillName, '%')))
              and (:interestName is null or lower(interest) like lower(concat('%', :interestName, '%')))
            """)
    Page<User> searchPublicProfiles(
            @Param("name") String name,
            @Param("college") String college,
            @Param("country") String country,
            @Param("skillName") String skillName,
            @Param("interestName") String interestName,
            @Param("role") Role role,
            Pageable pageable
    );
}
