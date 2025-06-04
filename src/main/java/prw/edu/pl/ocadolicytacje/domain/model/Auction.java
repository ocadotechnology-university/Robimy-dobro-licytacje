package prw.edu.pl.ocadolicytacje.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.ModeratorEntity;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.ParticipantEntity;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.SupplierEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Builder
@Setter
@Getter
public class Auction {

    Long auctionId;
    String title;

    LocalDateTime startDateTime;

    LocalDateTime endDateTime;

    String description;

    String photoUrl;

    String city;

    BigDecimal basePrice;

    String linkToThread;

    ParticipantEntity participantEntity;

    BigDecimal winPrice;

    ModeratorEntity moderatorEntity;

    SupplierEntity supplierEntity;

    String slackMessageTs;
    Boolean status;
    List<Bid> bids;

    public List<Bid> getBids() {
        return bids != null ? bids : Collections.emptyList();
    }
}
