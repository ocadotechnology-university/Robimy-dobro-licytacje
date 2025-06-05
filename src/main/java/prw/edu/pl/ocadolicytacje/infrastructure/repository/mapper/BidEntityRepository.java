package prw.edu.pl.ocadolicytacje.infrastructure.repository.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.BidEntity;

import java.util.List;

@Repository
public interface BidEntityRepository extends JpaRepository<BidEntity, Long> {
    List<BidEntity> findByAuctionEntity_AuctionId(Long auctionId);
}

