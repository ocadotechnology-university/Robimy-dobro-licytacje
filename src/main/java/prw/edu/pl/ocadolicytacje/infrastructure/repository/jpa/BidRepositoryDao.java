package prw.edu.pl.ocadolicytacje.infrastructure.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.BidEntity;

public interface BidRepositoryDao extends JpaRepository<BidEntity, Long> {
}

