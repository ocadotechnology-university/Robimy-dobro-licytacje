package prw.edu.pl.ocadolicytacje.infrastructure.repository.mapper;

import org.mapstruct.Mapper;
import prw.edu.pl.ocadolicytacje.domain.model.Participant;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.ParticipantEntity;

@Mapper(componentModel = "spring")
public interface ParticipantInfrastructureToDomainMapper {
    Participant map(ParticipantEntity participantEntity);
}
