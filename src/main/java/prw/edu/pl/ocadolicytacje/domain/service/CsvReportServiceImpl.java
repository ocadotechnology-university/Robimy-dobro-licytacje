package prw.edu.pl.ocadolicytacje.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import prw.edu.pl.ocadolicytacje.domain.model.Auction;
import prw.edu.pl.ocadolicytacje.domain.model.Bid;
import prw.edu.pl.ocadolicytacje.infrastructure.repository.AuctionRepository;
import prw.edu.pl.ocadolicytacje.infrastructure.repository.mapper.ParticipantEntityRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
@Service
public class CsvReportServiceImpl {

    private final AuctionRepository auctionRepository;
    private final ParticipantEntityRepository participantEntityRepository; // dodaj to

    public List<String> generateAuctionSummaryMessagesForDate(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<Auction> auctions = auctionRepository.findByEndDateTimeBetween(startOfDay, endOfDay);
        List<String> messages = new ArrayList<>();


        for (Auction auction : auctions) {
            System.out.println(auction.getBids());
            Bid highestBid = auction.getBids().stream()
                    .max(Comparator.comparing(Bid::getBidValue))
                    .orElse(null);

            System.out.println("highestBid: " + highestBid);

            String summary;
            if (highestBid != null) {
                // Pobieramy dane uczestnika z bazy po Slack ID
                AtomicReference<String> winnerName = new AtomicReference<>("Nieznany uczestnik");

                if (highestBid.getParticipantSlackId() != null) {
                    participantEntityRepository.findBySlackUserId(highestBid.getParticipantSlackId())
                            .ifPresent(participant ->
                                    winnerName.set(participant.getFirstName() + " " + participant.getLastName())
                            );
                }

                summary = String.format(
                        """
                        *🏁 Aukcja zakończona:* #%d – %s
                        👤 *Zwycięzca:* %s
                        💰 *Kwota:* %s zł
                        ⏰ *Zakończenie:* %s
                        """,
                        auction.getAuctionId(),
                        auction.getTitle(),
                        winnerName.get(),
                        highestBid.getBidValue(),
                        auction.getEndDateTime()
                );

            } else {
                summary = String.format(
                        """
                        *🏁 Aukcja zakończona:* #%d – %s
                        ❌ Brak ofert. Aukcja zakończona bez zwycięzcy.
                        ⏰ *Zakończenie:* %s
                        """,
                        auction.getAuctionId(),
                        auction.getTitle(),
                        auction.getEndDateTime()
                );
            }

            messages.add(summary);
        }

        return messages;
    }
}
