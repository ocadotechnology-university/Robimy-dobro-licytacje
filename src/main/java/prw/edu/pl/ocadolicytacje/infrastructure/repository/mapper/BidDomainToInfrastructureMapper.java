package prw.edu.pl.ocadolicytacje.infrastructure.repository.mapper;

import org.mapstruct.Mapper;
import prw.edu.pl.ocadolicytacje.domain.model.Bid;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.BidEntity;

@Mapper(componentModel = "spring")
public interface BidDomainToInfrastructureMapper {
    BidEntity map(Bid bid);
}
