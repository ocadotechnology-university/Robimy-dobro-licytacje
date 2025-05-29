package prw.edu.pl.ocadolicytacje.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Setter
@Getter
public class Bid {

    Long bidId;
    BigDecimal bidValue;
    LocalDateTime bidDateTime;
    Long auctionId;
    String participantSlackId;
}


