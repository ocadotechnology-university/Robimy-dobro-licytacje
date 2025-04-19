package prw.edu.pl.ocadolicytacje.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import prw.edu.pl.ocadolicytacje.domain.model.Bid;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.BidEntity;
import prw.edu.pl.ocadolicytacje.infrastructure.repository.jpa.BidRepositoryDao;
import prw.edu.pl.ocadolicytacje.infrastructure.repository.mapper.BidDomainToInfrastructureMapper;

@Repository
@RequiredArgsConstructor
public class BidRepository {

    private final BidRepositoryDao bidRepositoryDao;

    private final BidDomainToInfrastructureMapper bidDomainToInfrastructureMapper;

    public void save(Bid bid) {
        BidEntity bidEntityToSave = bidDomainToInfrastructureMapper.map(bid);
        bidRepositoryDao.save(bidEntityToSave);
    }
}
