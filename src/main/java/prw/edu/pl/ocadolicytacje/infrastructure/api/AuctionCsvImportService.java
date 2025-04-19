package prw.edu.pl.ocadolicytacje.infrastructure.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.AuctionEntity;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.ModeratorEntity;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.SupplierEntity;
import prw.edu.pl.ocadolicytacje.infrastructure.exception.DataRetrievalIOException;
import prw.edu.pl.ocadolicytacje.infrastructure.exception.GoogleSheetsSecurityException;
import prw.edu.pl.ocadolicytacje.infrastructure.repository.jpa.AuctionRepositoryDao;
import prw.edu.pl.ocadolicytacje.infrastructure.repository.jpa.ModeratorRepositoryDao;
import prw.edu.pl.ocadolicytacje.infrastructure.repository.jpa.SupplierRepositoryDao;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuctionCsvImportService {
    public static final String SECURITY_MESSAGE = "Wystąpił wyjątek związany z bezpieczeństwem, podczas pobierania danych z Google Sheet";
    public static final String DATA_RETRIEVAL_MESSAGE = "Wystąpił generalny wyjatek związany z odczytem danych z Google Sheet";
    private final ModeratorRepositoryDao moderatorRepositoryJpa;
    private final SupplierRepositoryDao supplierRepositoryDao;
    private final AuctionRepositoryDao auctionRepositoryDao;
    private final GoogleSheetReader googleSheetReader;
    private final AuctionSheetMapper auctionSheetMapper;

    public void saveImportedModeratorSupplierAuctionEntities() {

        List<List<Object>> csvData = null;
        try {
            csvData = googleSheetReader.getCsvData();
        } catch (GeneralSecurityException e) {
            log.warn(SECURITY_MESSAGE);
            throw new GoogleSheetsSecurityException(SECURITY_MESSAGE, e);
        } catch (IOException e) {
            log.warn(DATA_RETRIEVAL_MESSAGE);
            throw new DataRetrievalIOException(DATA_RETRIEVAL_MESSAGE, e);
        }
        List<AuctionCsvRowDto> auctionCsvRowDtoList = auctionSheetMapper.map(csvData);

        for (AuctionCsvRowDto auctionCsvRowDto : auctionCsvRowDtoList) {
            ModeratorEntity moderatorEntity = createModeratorEntity(auctionCsvRowDto.getModeratorFullName());
            moderatorRepositoryJpa.save(moderatorEntity);
            SupplierEntity supplierEntity = createSupplierEntity(auctionCsvRowDto.getSupplierFullName());
            supplierRepositoryDao.save(supplierEntity);
            AuctionEntity auctionEntity = createAuctionEntity(
                    auctionCsvRowDto.getAuctionTitle(),
                    auctionCsvRowDto.getAuctionDescription(),
                    auctionCsvRowDto.getPhotoUrl(),
                    auctionCsvRowDto.getCity(),
                    auctionCsvRowDto.getBasePrice(),
                    auctionCsvRowDto.getAuctionStartDateTime(),
                    auctionCsvRowDto.getAuctionEndDateTime()
            );
            auctionRepositoryDao.save(auctionEntity);
        }
    }

    private AuctionEntity createAuctionEntity(String auctionTitle, String auctionDescription, String photoUrl, String city, BigDecimal basePrice, LocalDateTime auctionStartDateTime, LocalDateTime auctionEndDateTime) {
        return AuctionEntity.builder()
                .title(auctionTitle)
                .description(auctionDescription)
                .photoUrl(photoUrl)
                .city(city)
                .basePrice(basePrice)
                .startDateTime(auctionStartDateTime)
                .endDateTime(auctionEndDateTime)
                .build();
    }

    private ModeratorEntity createModeratorEntity(String moderatorFullName) {
        final String[] fullNameSplited = moderatorFullName.trim().split("\\s+", 2);
        return ModeratorEntity.builder()
                .firstName(fullNameSplited[0])
                .lastName(fullNameSplited[1])
                .build();
    }

    private SupplierEntity createSupplierEntity(String supplierFullName) {
        final String[] fullNameSplited = supplierFullName.trim().split("\\s+", 2);
        return SupplierEntity.builder()
                .firstName(fullNameSplited[0])
                .lastName(fullNameSplited[1])
                .build();
    }

}
