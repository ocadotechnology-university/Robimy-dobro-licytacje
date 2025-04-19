package prw.edu.pl.ocadolicytacje.infrastructure.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.ModeratorEntity;

@Repository
public interface ModeratorRepositoryDao extends JpaRepository<ModeratorEntity, Long> {
}
