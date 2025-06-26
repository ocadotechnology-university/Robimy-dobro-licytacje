package prw.edu.pl.ocadolicytacje.infrastructure.repository.mapper;

import org.mapstruct.Mapper;
import prw.edu.pl.ocadolicytacje.domain.model.Bid;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.BidEntity;

@Mapper(componentModel = "spring")
public interface BidDomainToInfrastructureMapper {
    BidEntity map(Bid bid);

    public default Bid mapToDomain(BidEntity entity) {
        return Bid.builder()
                .bidId(entity.getBid_id())
                .bidValue(entity.getBidValue())
                .bidDateTime(entity.getBidDateTime())
                .auctionId(entity.getAuctionEntity().getAuctionId())
                .participantSlackId(entity.getParticipantEntity().getSlackUserId()) // ✅ TO JEST KLUCZ
                .build();
    }

}
