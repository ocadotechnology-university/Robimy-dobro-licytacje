package prw.edu.pl.ocadolicytacje.infrastructure.repository.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.BidEntity;

@Repository
public interface BidEntityRepository extends JpaRepository<BidEntity, Long> {
    // JpaRepository już zawiera metodę save(BidEntity bidEntity)
}

