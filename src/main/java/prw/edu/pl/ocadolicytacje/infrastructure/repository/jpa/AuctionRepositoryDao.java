package prw.edu.pl.ocadolicytacje.infrastructure.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import prw.edu.pl.ocadolicytacje.domain.model.Auction;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.AuctionEntity;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuctionRepositoryDao extends JpaRepository<AuctionEntity, Long> {
    List<AuctionEntity> findAllByStartDateTimeBetween(LocalDateTime auctionStartDateTime, LocalDateTime auctionEndDateTime);

    List<AuctionEntity> findByStatusFalseAndStartDateTime(LocalDateTime dateTime);

    List<AuctionEntity> findByEndDateTime(LocalDateTime dateTime);


    List<AuctionEntity> findByEndDateTimeBetween(LocalDateTime startOfDay, LocalDateTime endOfDay);

    @Query("SELECT a FROM AuctionEntity a LEFT JOIN FETCH a.bidEntityList WHERE a.auctionId = :auctionId")
    AuctionEntity findByIdWithBids(@Param("auctionId") Long auctionId);

}
