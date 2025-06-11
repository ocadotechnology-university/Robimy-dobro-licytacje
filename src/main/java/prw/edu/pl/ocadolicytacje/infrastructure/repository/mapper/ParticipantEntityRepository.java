package prw.edu.pl.ocadolicytacje.infrastructure.repository.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.ParticipantEntity;

import java.util.Optional;

@Repository
public interface ParticipantEntityRepository extends JpaRepository<ParticipantEntity, Long> {

    Optional<ParticipantEntity> findBySlackUserId(String slackUserId);

    // Metoda save(ParticipantEntity participantEntity) jest dziedziczona z JpaRepository
}

