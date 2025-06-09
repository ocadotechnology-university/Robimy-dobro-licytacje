package prw.edu.pl.ocadolicytacje.infrastructure.repository.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.AuctionEntity;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.BidEntity;

import java.util.List;

@Repository
public interface AuctionEntityRepository extends JpaRepository<AuctionEntity, Long> {

}


