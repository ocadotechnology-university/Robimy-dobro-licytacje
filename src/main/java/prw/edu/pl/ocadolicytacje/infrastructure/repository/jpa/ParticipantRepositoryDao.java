package prw.edu.pl.ocadolicytacje.infrastructure.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.ParticipantEntity;

@Repository
public interface ParticipantRepositoryDao extends JpaRepository<ParticipantEntity, Long> {
    ParticipantEntity findBySlackUserId(String slackUserId);
}
