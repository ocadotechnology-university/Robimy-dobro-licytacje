package prw.edu.pl.ocadolicytacje.domain.service;

import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import prw.edu.pl.ocadolicytacje.domain.model.Auction;
import prw.edu.pl.ocadolicytacje.domain.model.Bid;
import prw.edu.pl.ocadolicytacje.infrastructure.repository.AuctionRepository;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CsvReportServiceImpl {
    private final AuctionRepository auctionRepository;


    public File generateCsvReportForDate(LocalDate date) throws IOException {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<Auction> auctions = auctionRepository.findByEndDateTimeBetween(startOfDay, endOfDay);

        File file = File.createTempFile("raport_" + date, ".csv");

        try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
            writer.writeNext(new String[]{"ID Aukcji", "Zwycięzca", "Kwota", "Data zakończenia"});
            for (Auction auction : auctions) {
                Bid highestBid = auction.getBids().stream()
                        .max(Comparator.comparing(Bid::getBidValue))
                        .orElse(null);
                if (highestBid != null) {
                    writer.writeNext(new String[]{
                            String.valueOf(auction.getAuctionId()),
                            auction.getParticipantEntity().getFirstName() + " " + auction.getParticipantEntity().getLastName(),
                            highestBid.getBidValue().toString(),
                            auction.getEndDateTime().toString()
                    });
                } else {
                    writer.writeNext(new String[]{
                            String.valueOf(auction.getAuctionId()),
                            "Brak zwycięzcy",
                            "0",
                            auction.getEndDateTime().toString()
                    });
                }
            }
        }

        return file;
    }
}
