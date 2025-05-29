package prw.edu.pl.ocadolicytacje.infrastructure.repository;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import prw.edu.pl.ocadolicytacje.domain.model.Auction;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.AuctionEntity;
import prw.edu.pl.ocadolicytacje.infrastructure.repository.jpa.AuctionRepositoryDao;
import prw.edu.pl.ocadolicytacje.infrastructure.repository.mapper.AuctionDomainToInfrastructureMapper;
import prw.edu.pl.ocadolicytacje.infrastructure.repository.mapper.AuctionInfrastructureToDomainMapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AuctionRepository {

    private final AuctionRepositoryDao auctionRepositoryDao;

    private final AuctionInfrastructureToDomainMapper auctionInfrastructureToDomainMapper;

    private final AuctionDomainToInfrastructureMapper auctionDomainToInfrastructureMapper;


    public List<Auction> findAllByStartDateTimeBetween(@NonNull final LocalDateTime auctionStartDateTime, @NonNull final LocalDateTime auctionEndDateTime) {
        List<AuctionEntity> allByStartDateTimeBetween = auctionRepositoryDao.findAllByStartDateTimeBetween(auctionStartDateTime, auctionEndDateTime);
        return auctionInfrastructureToDomainMapper.map(allByStartDateTimeBetween);
    }


    public void saveAll(@NonNull final List<Auction> allByStartDateTimeBetween) {
        Collection<AuctionEntity> mapped = auctionDomainToInfrastructureMapper.map(allByStartDateTimeBetween);
        auctionRepositoryDao.saveAll(mapped);
    }

    public List<Auction> findByStatusFalseAndStartDateTime(@NonNull final LocalDateTime startOfToday) {
        List<AuctionEntity> statusFalseAndStartDateTime = auctionRepositoryDao.findByStatusFalseAndStartDateTime(startOfToday);
        return auctionInfrastructureToDomainMapper.map(statusFalseAndStartDateTime);
    }


    public Auction findById(@NonNull final Long auctionId) {
        AuctionEntity auctionEntity = auctionRepositoryDao.findById(auctionId)
                .orElseThrow(() -> new IllegalStateException("Auction with provided I does not exist: " + auctionId));
        return auctionInfrastructureToDomainMapper.map(auctionEntity);
    }

    public List<Auction> findAll() {
        List<AuctionEntity> allAuctions = auctionRepositoryDao.findAll();
        return auctionInfrastructureToDomainMapper.map(allAuctions);
    }


    public List<Auction> findByEndDateTimeBetween(LocalDateTime startOfDay, LocalDateTime endOfDay) {
        List<AuctionEntity> byEndDateTimeBetween = auctionRepositoryDao.findByEndDateTimeBetween(startOfDay, endOfDay);
        return auctionInfrastructureToDomainMapper.map(byEndDateTimeBetween);
    }
}
