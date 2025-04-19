package prw.edu.pl.ocadolicytacje.infrastructure.repository.mapper;

import org.mapstruct.Mapper;
import prw.edu.pl.ocadolicytacje.domain.model.Auction;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.AuctionEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AuctionInfrastructureToDomainMapper {

    Auction map(AuctionEntity auctionEntity);

    List<Auction> map(List<AuctionEntity> auctionEntity);

}
