package prw.edu.pl.ocadolicytacje.domain.service;

import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.bolt.context.builtin.ViewSubmissionContext;
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


    public void createAuctionThreadAndPostOnChannel(@NonNull final LocalDate startDate,
                                                    @NonNull final SlashCommandContext context) throws SlackApiException, IOException {
        auctionCsvImportService.saveImportedModeratorSupplierAuctionEntities();

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime= startDate.atTime(LocalTime.MAX);

        final List<Auction> allByStartDateTimeBetween = auctionRepository.findAllByStartDateTimeBetween(startDateTime, endDateTime);
        for (Auction auction : allByStartDateTimeBetween) {
            final String channelId = slackProperties.getChannelId();
            final String threadTs = slackAuctionThreadServiceImpl.postAuctionToSlack(auction, context, channelId);
            final String threadLink = "https://slack.com/app_redirect?channel=" + channelId + "&thread_ts=" + threadTs;
            auction.setLinkToThread(threadLink);
            auction.setSlackMessageTs(threadTs);
        }
        auctionRepository.saveAll(allByStartDateTimeBetween);

    }

    public void createAuctionThreadAndPostOnChannel(@NonNull final LocalDate startDate,
                                                    @NonNull LocalTime startTime,
                                                    @NonNull LocalTime endTime,
                                                    @NonNull final ViewSubmissionContext context) throws SlackApiException, IOException {
        auctionCsvImportService.saveImportedModeratorSupplierAuctionEntities();

        LocalDateTime startDateTime = startDate.atTime(startTime);
        LocalDateTime endDateTime = startDate.atTime(endTime);

        final List<Auction> allByStartDateTimeBetween = auctionRepository.findAllByStartDateTimeBetween(startDateTime, endDateTime);
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
