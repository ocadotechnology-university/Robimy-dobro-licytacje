package prw.edu.pl.ocadolicytacje.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.ModeratorEntity;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.ParticipantEntity;

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

    String supplierFullName;

    String slackMessageTs;
    Boolean status;

    @OneToMany(mappedBy = "auctionEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Bid> bids;

    public List<Bid> getBids() {
        return bids != null ? bids : Collections.emptyList();
    }
}
