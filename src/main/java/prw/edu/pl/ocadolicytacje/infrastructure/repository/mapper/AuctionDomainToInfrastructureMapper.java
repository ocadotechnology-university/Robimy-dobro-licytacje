package prw.edu.pl.ocadolicytacje.infrastructure.repository.mapper;

import org.mapstruct.Mapper;
import prw.edu.pl.ocadolicytacje.domain.model.Auction;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.AuctionEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AuctionDomainToInfrastructureMapper {

    AuctionEntity map(Auction auctionEntity);

    List<AuctionEntity> map(List<Auction> auctionEntity);


}
