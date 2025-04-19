package prw.edu.pl.ocadolicytacje.infrastructure.repository;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import prw.edu.pl.ocadolicytacje.domain.model.Participant;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.ParticipantEntity;
import prw.edu.pl.ocadolicytacje.infrastructure.repository.jpa.ParticipantRepositoryDao;
import prw.edu.pl.ocadolicytacje.infrastructure.repository.mapper.ParticipantInfrastructureToDomainMapper;

@Repository
@RequiredArgsConstructor
public class ParticipantRepository {

    @NonNull
    private final ParticipantRepositoryDao participantRepositoryDao;
    @NonNull
    private final ParticipantInfrastructureToDomainMapper participantInfrastructureToDomainMapper;

    public Participant findBySlackUserId(String participantSlackId) {
        ParticipantEntity bySlackUserId = participantRepositoryDao.findBySlackUserId(participantSlackId);
        return participantInfrastructureToDomainMapper.map(bySlackUserId);
    }
}
