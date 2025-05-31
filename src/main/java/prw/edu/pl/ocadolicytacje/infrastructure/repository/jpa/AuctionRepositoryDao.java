package prw.edu.pl.ocadolicytacje.infrastructure.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.AuctionEntity;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuctionRepositoryDao extends JpaRepository<AuctionEntity, Long> {
    List<AuctionEntity> findAllByStartDateTimeBetween(LocalDateTime auctionStartDateTime, LocalDateTime auctionEndDateTime);

    List<AuctionEntity> findByStatusFalseAndStartDateTime(LocalDateTime dateTime);

    List<AuctionEntity> findByEndDateTime(LocalDateTime dateTime);


    List<AuctionEntity> findByEndDateTimeBetween(LocalDateTime startOfDay, LocalDateTime endOfDay);
}
