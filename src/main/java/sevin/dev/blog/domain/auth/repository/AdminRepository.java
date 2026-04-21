package sevin.dev.blog.domain.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sevin.dev.blog.domain.auth.entity.Admin;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByGithubUsername(String githubUsername);

    boolean existsByGithubUsername(String githubUsername);
}
