package prw.edu.pl.ocadolicytacje.domain.service;

import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.methods.SlackApiException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import prw.edu.pl.ocadolicytacje.domain.model.Auction;
import prw.edu.pl.ocadolicytacje.domain.service.port.AuctionActivationService;
import prw.edu.pl.ocadolicytacje.infrastructure.repository.AuctionRepository;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuctionActivationServiceImpl implements AuctionActivationService {

    private final AuctionRepository auctionRepository;
    private final SlackAuctionThreadServiceImpl slackAuctionThreadServiceImpl;

    private final Clock clock;

    @Override
    @Scheduled(cron = "0 25 15 * * *")
    @Transactional
    public void activateScheduledAuction() throws SlackApiException, IOException {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        LocalDateTime startOfToday = today.atTime(0, 0);
        LocalDateTime endOfToday = today.atTime(23, 59);

        List<Auction> auctionsToActivate = auctionRepository.findAllByStartDateTimeBetween(startOfToday, endOfToday);

        for (Auction auction : auctionsToActivate) {
            auction.setStatus(true);
            auctionRepository.save(auction);

            slackAuctionThreadServiceImpl.updateSlackAuctionStatus(auction);
        }
    }

    public void activateAuctionManually(SlashCommandContext ctx) throws SlackApiException, IOException {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        LocalDateTime startOfToday = today.atTime(0, 0);
        LocalDateTime endOfToday = today.atTime(23, 59);

        List<Auction> auctionsToActivate = auctionRepository.findAllByStartDateTimeBetween(startOfToday, endOfToday);

        if (auctionsToActivate.isEmpty()) {
            ctx.respond("Brak aukcji do aktywacji na dzisiaj.");
            return;
        }

        for (Auction auction : auctionsToActivate) {
            auction.setStatus(true);
            auctionRepository.save(auction);

            slackAuctionThreadServiceImpl.updateSlackAuctionStatus(ctx, auction);
        }
        ctx.respond("Aukcje zostały aktywowane ręcznie.");
    }

    public void deactivateAuctionManually(SlashCommandContext ctx) throws SlackApiException, IOException {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        LocalDateTime startOfToday = today.atTime(0, 0);
        LocalDateTime endOfToday = today.atTime(23, 59);

        List<Auction> auctionsToActivate = auctionRepository.findAllByStartDateTimeBetween(startOfToday, endOfToday);

        if (auctionsToActivate.isEmpty()) {
            ctx.respond("Brak aukcji do dezaktywacji na dzisiaj.");
            return;
        }

        for (Auction auction : auctionsToActivate) {
            auction.setStatus(false);
            auctionRepository.save(auction);

            slackAuctionThreadServiceImpl.updateSlackAuctionStatus(ctx, auction);
        }
        ctx.respond("Aukcje zostały dezaktywowane ręcznie.");
    }

    @Override
    @Scheduled(cron = "0 24 15 * * *")
    @Transactional
    public void deactivateScheduledAuction() throws SlackApiException, IOException {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        LocalDateTime startOfToday = today.atTime(0, 0);
        LocalDateTime endOfToday = today.atTime(23, 59);

        List<Auction> auctionsToActivate = auctionRepository.findAllByStartDateTimeBetween(startOfToday, endOfToday);

        for (Auction auction : auctionsToActivate) {
            auction.setStatus(false);
            auctionRepository.save(auction);

            slackAuctionThreadServiceImpl.updateSlackAuctionStatus(auction);
        }
    }

}
