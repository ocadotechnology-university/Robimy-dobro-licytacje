package prw.edu.pl.ocadolicytacje.infrastructure.repository.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.AuctionEntity;

@Repository
public interface AuctionEntityRepository extends JpaRepository<AuctionEntity, Long> {
    // JpaRepository już zawiera findById(Long id)
}

