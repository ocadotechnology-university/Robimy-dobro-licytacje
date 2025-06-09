package prw.edu.pl.ocadolicytacje.domain.service;

import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.methods.SlackApiException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import prw.edu.pl.ocadolicytacje.domain.model.Auction;
import prw.edu.pl.ocadolicytacje.domain.model.Bid;
import prw.edu.pl.ocadolicytacje.domain.model.Participant;
import prw.edu.pl.ocadolicytacje.domain.service.port.AuctionEndService;
import prw.edu.pl.ocadolicytacje.infrastructure.repository.AuctionRepository;
import prw.edu.pl.ocadolicytacje.infrastructure.repository.ParticipantRepository;
import prw.edu.pl.ocadolicytacje.slack.SlackProperties;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuctionEndServiceImpl implements AuctionEndService {

    private final AuctionRepository auctionRepository;
    private final SlackAuctionThreadServiceImpl slackAuctionThreadServiceImpl;
    private final ParticipantRepository participantEntityRepository;
    private final SlackProperties slackProperties;
    private final Clock clock;



    @Transactional
    public void endAuctionsManual(SlashCommandContext ctx) throws IOException, SlackApiException {
        endAuctionsInternal(ctx);
        if (ctx != null) {
            ctx.respond("Aukcje zostały zakończone ręcznie.");
        }
    }

     void endAuctionsInternal(SlashCommandContext ctx) throws IOException, SlackApiException {
        LocalDateTime now = LocalDateTime.now(clock);

        List<Auction> auctionsToEnd = auctionRepository.findAll().stream()
                .filter(a -> a.getEndDateTime().isBefore(now) && a.getStatus())
                .collect(Collectors.toList());

        StringBuilder summary = new StringBuilder("Podsumowanie dzisiejszych licytacji:\n\n");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String endDate = now.toLocalDate().format(formatter);

        for (Auction auction : auctionsToEnd) {
            auction.setStatus(false);

            if (auction.getBids() != null && !auction.getBids().isEmpty()) {
                Bid highestBid = auction.getBids().stream()
                        .max(Comparator.comparing(Bid::getBidValue))
                        .orElse(null);

                if (highestBid != null) {
                    String participantSlackId = highestBid.getParticipantSlackId();
                    Participant winner = participantEntityRepository.findBySlackUserId(participantSlackId);

                    summary.append("AUKCJA *").append(auction.getAuctionId())
                            .append("* - zwycięzca: *").append(winner.getFirstName()).append(" ").append(winner.getLastName())
                            .append("* - ").append(highestBid.getBidValue()).append(" zł\n");

                    if (ctx != null) {
                        slackAuctionThreadServiceImpl.sendPrivateWinMessage(winner, auction, ctx);
                    }
                }
            } else {
                summary.append("AUKCJA *").append(auction.getAuctionId()).append("* - brak propozycji\n");
            }

            slackAuctionThreadServiceImpl.updateSlackAuctionStatus(null, auction);
        }

        summary.append("\nWszystkim zwycięzcom gratulujemy 👏 🎉\n")
                .append("\nKwotę należy wpłacić na konto:\n")
                .append("\n*[dane]*\n")
                .append("\ntytuł: Ocado RobimyDobro ").append(now.getYear())
                .append(" - [data wystawienia aukcji] / [numer aukcji]\n")
                .append("\nOstateczny termin wpłaty to ").append(endDate).append("\n")
                .append("\nW celu odebrania wygranej proszę o bezpośredni kontakt z wystawiającym aukcję.\n")
                .append("\nDziękujemy bardzo za moderację wystawionych aukcji.");

        if (ctx != null) {
            slackAuctionThreadServiceImpl.sendSummaryToChannel(auctionsToEnd, ctx);
        }

        auctionRepository.saveAll(auctionsToEnd);
    }
}
















