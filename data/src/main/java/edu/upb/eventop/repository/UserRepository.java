package edu.upb.eventop.repository;

import edu.upb.eventop.repository.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsernameIgnoreCase(String lowerCase);

    @Query("SELECT new edu.upb.eventop.repository.entity.User(u.id, u.username, u.role) FROM User u WHERE  u.id=:pId")
    Optional<User> findByUserIdToValidateSession(@Param("pId") String pId);
}
