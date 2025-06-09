package prw.edu.pl.ocadolicytacje.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import prw.edu.pl.ocadolicytacje.domain.service.port.SlackAuctionThreadService;
import prw.edu.pl.ocadolicytacje.infrastructure.repository.AuctionRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionEndScheduler {

    private final AuctionRepository auctionRepository;
    private final SlackAuctionThreadService slackService;
    private final TaskScheduler taskScheduler;


    @EventListener(ContextRefreshedEvent.class)
    public void scheduleExistingAuctions() {

    }

}
