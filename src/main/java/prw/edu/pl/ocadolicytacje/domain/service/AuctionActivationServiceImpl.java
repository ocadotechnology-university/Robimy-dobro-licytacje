package prw.edu.pl.ocadolicytacje.domain.service;

import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.methods.SlackApiException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import prw.edu.pl.ocadolicytacje.domain.model.Auction;
import prw.edu.pl.ocadolicytacje.domain.service.port.AuctionActivationService;
import prw.edu.pl.ocadolicytacje.infrastructure.repository.AuctionRepository;

import java.io.IOException;
import java.time.*;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuctionActivationServiceImpl implements AuctionActivationService {

    private final AuctionRepository auctionRepository;
    private final SlackAuctionThreadServiceImpl slackAuctionThreadServiceImpl;

    private final Clock clock;
    private final AuctionEndScheduler auctionEndScheduler;
    private final TaskScheduler taskScheduler;


    @EventListener(org.springframework.context.event.ContextRefreshedEvent.class)
    public void scheduleExistingAuctions() {
        LocalDateTime now = LocalDateTime.now(clock);

        auctionRepository.findAll()
                .stream()
                .filter(a -> Boolean.TRUE.equals(a.getStatus()))
                .filter(a -> a.getEndDateTime().isAfter(now))
                .forEach(this::scheduleEnd);
    }

    public ScheduledFuture<?> scheduleEnd(Auction auction) {
        LocalDateTime endsAt = auction.getEndDateTime();
        Instant executionTime = endsAt.atZone(clock.getZone()).toInstant(); // <- poprawna konwersja

        Duration delay = Duration.between(Instant.now(clock), executionTime);

        if (delay.isNegative()) {
            log.warn("Aukcja {} już minęła, kończę natychmiast", auction.getAuctionId());
            return taskScheduler.schedule(() -> handleEnd(auction), Instant.now(clock).plusSeconds(1));
        }

        log.info("Planowanie zakończenia aukcji {} na {}", auction.getAuctionId(), executionTime);
        return taskScheduler.schedule(() -> handleEnd(auction), executionTime);
    }

    private void handleEnd(Auction auction) {
        try {
            slackAuctionThreadServiceImpl.finishAuctionOnSlack(auction);      // patrz punkt 2
            auction.setStatus(false);
            auctionRepository.save(auction);
            log.info("Aukcja {} zakończona", auction.getAuctionId());
        } catch (Exception ex) {
            log.error("Błąd przy kończeniu aukcji {}", auction.getAuctionId(), ex);
        }
    }

    @Override
    @Transactional
    public void activateScheduledAuction() throws SlackApiException, IOException {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        LocalDateTime startOfToday = today.atTime(6, 0);

        List<Auction> auctionsToActivate = auctionRepository.findByStatusFalseAndStartDateTime(startOfToday);

        for (Auction auction : auctionsToActivate) {
            auction.setStatus(true);
            auctionRepository.save(auction);
            slackAuctionThreadServiceImpl.updateSlackAuctionStatus(auction);
        }
        scheduleExistingAuctions();
    }

    public void activateAuctionManually(SlashCommandContext ctx) throws SlackApiException, IOException {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        LocalDateTime startOfToday = today.atTime(6, 0);

        List<Auction> auctionsToActivate = auctionRepository.findByStatusFalseAndStartDateTime(startOfToday);

        for (Auction auction : auctionsToActivate) {
            auction.setStatus(true);
            auctionRepository.save(auction);

            slackAuctionThreadServiceImpl.updateSlackAuctionStatus(ctx, auction);
        }
        ctx.respond("Aukcje zostały aktywowane ręcznie.");
    }


}
