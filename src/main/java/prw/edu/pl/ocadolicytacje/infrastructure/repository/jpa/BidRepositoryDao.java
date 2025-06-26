package prw.edu.pl.ocadolicytacje.infrastructure.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.BidEntity;

import java.util.List;

public interface BidRepositoryDao extends JpaRepository<BidEntity, Long> {
    @Query("SELECT b FROM BidEntity b WHERE b.auctionEntity.auctionId = :auctionId")
    List<BidEntity> findAllByAuctionId(@Param("auctionId") Long auctionId);
}

