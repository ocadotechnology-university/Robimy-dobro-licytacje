package prw.edu.pl.ocadolicytacje.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "auction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuctionEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "auction_id")
    private Long auctionId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "start_date_time", nullable = false)
    private LocalDateTime startDateTime;

    @Column(name = "end_date_time", nullable = false)
    private LocalDateTime endDateTime;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "photo_url", nullable = false, length = 512)
    private String photoUrl;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "link_to_thread", nullable = false)
    private String linkToThread;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private Boolean status = false;

    @ManyToOne
    @JoinColumn(name = "fk_winner_id")
    private ParticipantEntity winner;

    @Column(name = "win_price", precision = 10, scale = 2)
    private BigDecimal winPrice;

    @ManyToOne
    @JoinColumn(name = "moderator_id")
    private ModeratorEntity moderatorEntity;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private SupplierEntity supplierEntity;

    @Column(name = "slack_message_ts")
    private String slackMessageTs;

    @OneToMany
    @Column(name = "bid")
    private List<BidEntity> bidEntityList;

}
