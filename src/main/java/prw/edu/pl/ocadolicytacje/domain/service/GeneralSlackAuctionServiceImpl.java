package prw.edu.pl.ocadolicytacje.domain.service;

import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.methods.SlackApiException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import prw.edu.pl.ocadolicytacje.domain.model.Auction;
import prw.edu.pl.ocadolicytacje.domain.service.port.GeneralSlackAuctionService;
import prw.edu.pl.ocadolicytacje.infrastructure.api.AuctionCsvImportService;
import prw.edu.pl.ocadolicytacje.infrastructure.repository.AuctionRepository;
import prw.edu.pl.ocadolicytacje.slack.SlackProperties;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GeneralSlackAuctionServiceImpl implements GeneralSlackAuctionService {

    private final AuctionCsvImportService auctionCsvImportService;

    private final AuctionRepository auctionRepository;

    private final SlackAuctionThreadServiceImpl slackAuctionThreadServiceImpl;

    private final SlackProperties slackProperties;


    public void createAuctionThreadAndPostOnChannel(@NonNull final LocalDate date, @NonNull final SlashCommandContext context) throws SlackApiException, IOException {
        auctionCsvImportService.saveImportedModeratorSupplierAuctionEntities();
        final LocalDateTime startOfDay = date.atStartOfDay();
        final LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        final List<Auction> allByStartDateTimeBetween = auctionRepository.findAllByStartDateTimeBetween(startOfDay, endOfDay);
        for (Auction auction : allByStartDateTimeBetween) {
            final String channelId = slackProperties.getChannelId();
            final String threadTs = slackAuctionThreadServiceImpl.postAuctionToSlack(auction, context, channelId);
            final String threadLink = "https://slack.com/app_redirect?channel=" + channelId + "&thread_ts=" + threadTs;
            auction.setLinkToThread(threadLink);
            auction.setSlackMessageTs(threadTs);
        }
        auctionRepository.saveAll(allByStartDateTimeBetween);

    }


}
