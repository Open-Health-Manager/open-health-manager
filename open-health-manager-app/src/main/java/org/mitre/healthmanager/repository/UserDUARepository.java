package org.mitre.healthmanager.repository;

import java.util.List;
import java.util.Optional;
import org.mitre.healthmanager.domain.UserDUA;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data SQL repository for the UserDUA entity.
 */
@Repository
public interface UserDUARepository extends JpaRepository<UserDUA, Long> {
    @Query("select userDUA from UserDUA userDUA where userDUA.user.login = ?#{principal.username}")
    List<UserDUA> findByUserIsCurrentUser();

    default Optional<UserDUA> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<UserDUA> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<UserDUA> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select distinct userDUA from UserDUA userDUA left join fetch userDUA.user",
        countQuery = "select count(distinct userDUA) from UserDUA userDUA"
    )
    Page<UserDUA> findAllWithToOneRelationships(Pageable pageable);

    @Query("select distinct userDUA from UserDUA userDUA left join fetch userDUA.user")
    List<UserDUA> findAllWithToOneRelationships();

    @Query("select userDUA from UserDUA userDUA left join fetch userDUA.user where userDUA.id =:id")
    Optional<UserDUA> findOneWithToOneRelationships(@Param("id") Long id);

    @Query("select distinct userDUA from UserDUA userDUA where user_id =:id")
    List<UserDUA> findAllForUser(@Param("id") Long id);

}
