package prw.edu.pl.ocadolicytacje.domain.service.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slack.api.bolt.context.builtin.ViewSubmissionContext;
import com.slack.api.bolt.handler.builtin.ViewSubmissionHandler;
import com.slack.api.bolt.request.builtin.ViewSubmissionRequest;
import com.slack.api.bolt.response.Response;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.view.ViewState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import prw.edu.pl.ocadolicytacje.domain.model.Auction;
import prw.edu.pl.ocadolicytacje.domain.model.Bid;
import prw.edu.pl.ocadolicytacje.domain.service.SlackAuctionThreadServiceImpl;
import prw.edu.pl.ocadolicytacje.domain.service.port.SlackAuctionThreadService;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.AuctionEntity;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.BidEntity;
import prw.edu.pl.ocadolicytacje.infrastructure.entity.ParticipantEntity;
import prw.edu.pl.ocadolicytacje.infrastructure.repository.AuctionRepository;
import prw.edu.pl.ocadolicytacje.infrastructure.repository.mapper.AuctionEntityRepository;
import prw.edu.pl.ocadolicytacje.infrastructure.repository.mapper.BidEntityRepository;
import prw.edu.pl.ocadolicytacje.infrastructure.repository.mapper.ParticipantEntityRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class BidModalHandler implements ViewSubmissionHandler {

    private final SlackAuctionThreadServiceImpl slackAuctionThreadService;
    private final BidEntityRepository bidEntityRepository;
    private final ParticipantEntityRepository participantEntityRepository;
    private final AuctionEntityRepository auctionEntityRepository;
    private final AuctionRepository auctionRepository;

    @Override
    public Response apply(ViewSubmissionRequest req, ViewSubmissionContext ctx) {
        log.info("BidModalHandler.apply() wywołane");
        try {
            ViewState inputViewState = req.getPayload().getView().getState();
            JsonNode metaData = getRequestMetaData(req);

            BigDecimal inputBidValue = getInputBidValue(inputViewState);
            Long auctionId = metaData.get("auctionId").asLong();
            String userId = req.getPayload().getUser().getId();

            Auction auction = auctionRepository.findById(auctionId);
            if (auction == null) {
                throw new IllegalStateException("Auction not found with id: " + auctionId);
            }

            if (!auction.getStatus()) {
                return ctx.ackWithErrors(Collections.singletonMap("bid_value", "Aukcja jest nieaktywna."));
            }

            List<BidEntity> bidEntities = bidEntityRepository.findByAuctionEntity_AuctionId(auctionId);

            BigDecimal highestBid = bidEntities.stream()
                    .map(BidEntity::getBidValue)
                    .max(Comparator.naturalOrder())
                    .orElse(auction.getBasePrice());

            if (inputValueIsLowerThanActual(inputBidValue, highestBid)) {
                log.info("Provided price: {} is lower than actual highest price: {}", inputBidValue, highestBid);
                String errorMessage = "Wprowadzona kwota %s jest niższa, od aktualnej kwoty licytacji: %s  ".formatted(inputBidValue, highestBid);

                // Wysyłamy wiadomość do kanału z informacją
                try {
                    ctx.client().chatPostEphemeral(r -> r
                            .channel(metaData.get("channelId").asText())
                            .user(req.getPayload().getUser().getId())  // wskazujemy użytkownika, który ma widzieć wiadomość
                            .text(String.format(
                                    "Podaj kwotę wyższą niż %s",
                                    highestBid
                            )));
                } catch (Exception slackEx) {
                    log.warn("Nie udało się wysłać powiadomienia o zbyt niskiej ofercie: {}", slackEx.getMessage());
                }

                return ctx.ackWithErrors(Collections.singletonMap("bid_value", errorMessage));
            }

            AuctionEntity auctionEntity = auctionEntityRepository.findById(auctionId)
                    .orElseThrow(() -> new IllegalStateException("AuctionEntity not found with id: " + auctionId));

            ParticipantEntity participantEntity = participantEntityRepository
                    .findBySlackUserId(userId)
                    .orElseGet(() -> {
                        log.info("Użytkownik {} nie istnieje – próbujemy pobrać dane z Slacka", userId);

                        String firstName = "Nieznane";
                        String lastName = "Nieznane";

                        try {
                            var userInfo = ctx.client().usersInfo(r -> r.user(userId));
                            if (userInfo.isOk()) {
                                var profile = userInfo.getUser().getProfile();

                                if (profile.getFirstName() != null && !profile.getFirstName().isEmpty()) {
                                    firstName = profile.getFirstName();
                                }

                                if (profile.getLastName() != null && !profile.getLastName().isEmpty()) {
                                    lastName = profile.getLastName();
                                }
                            } else {
                                log.warn("Nie udało się pobrać danych użytkownika ze Slacka: {}", userInfo.getError());
                            }
                        } catch (Exception ex) {
                            log.error("Błąd podczas pobierania danych użytkownika z Slacka", ex);
                        }

                        ParticipantEntity newParticipant = new ParticipantEntity();
                        newParticipant.setSlackUserId(userId);
                        newParticipant.setFirstName(firstName);
                        newParticipant.setLastName(lastName);
                        newParticipant.setCity("Nieznane"); // Możesz uzupełnić inaczej, jeśli masz dostęp do lokalizacji

                        return participantEntityRepository.save(newParticipant);
                    });

            BidEntity bidEntity = new BidEntity();
            bidEntity.setAuctionEntity(auctionEntity);
            bidEntity.setParticipantEntity(participantEntity);
            bidEntity.setBidValue(inputBidValue);
            bidEntity.setBidDateTime(LocalDateTime.now());

            bidEntityRepository.save(bidEntity); // użyj JpaRepository<BidEntity, Long>

            // Pobierz aktualizowaną aukcję (z nową listą bidów, włącznie z nowym bidem)
//            Auction updatedAuction = auctionRepository.findById(auctionId);
            Auction updatedAuction = auctionRepository.findByIdWithBids(auctionId);
            if (updatedAuction == null) {
                throw new IllegalStateException("Auction not found with id: " + auctionId);
            }

            // Aktualizacja wiadomości Slack
            String channelId = metaData.get("channelId").asText();
            String messageTs = updatedAuction.getSlackMessageTs();

            log.info("Znaleziono {} bidów dla aukcji {}",
                    updatedAuction.getBids().size(), updatedAuction.getAuctionId());

            slackAuctionThreadService.refreshPriceOnSlack(updatedAuction);
            slackAuctionThreadService.sendBidToast(userId, inputBidValue, updatedAuction);


            BidEntity previousHighestBid = bidEntities.stream()
                    .max(Comparator.comparing(BidEntity::getBidValue))
                    .orElse(null);

            if (previousHighestBid != null &&
                    !previousHighestBid.getParticipantEntity().getSlackUserId().equals(userId)) {

                String outbidMessage = String.format(
                        "<@%s> Twoja oferta została przebita przez <@%s>. Nowa cena: %s zł",
                        previousHighestBid.getParticipantEntity().getSlackUserId(),
                        userId,
                        inputBidValue
                );

                ctx.client().chatPostMessage(r -> r
                        .channel(metaData.get("channelId").asText())
                        .threadTs(metaData.get("messageTs").asText())
                        .text(outbidMessage));
            }

            return ctx.ack();
        } catch (Exception e) {
            return ctx.ackWithErrors(Collections.singletonMap("bid_value", "Błąd: " + e.getMessage()));
        }
    }

    private Bid buildBid(Long auctionId, String user, BigDecimal inputBidValue) {
        return Bid.builder()
                .auctionId(auctionId)
                .participantSlackId(user)
                .bidValue(inputBidValue)
                .bidDateTime(LocalDateTime.now())
                .build();
    }

    private boolean inputValueIsLowerThanActual(BigDecimal inputBidValue, BigDecimal highestBid) {
        return inputBidValue.compareTo(highestBid) <= 0;
    }

    private JsonNode getRequestMetaData(ViewSubmissionRequest req) throws JsonProcessingException {
        var meta = new ObjectMapper().readTree(req.getPayload().getView().getPrivateMetadata());
        return meta;
    }

    private BigDecimal getInputBidValue(ViewState inputViewState) {
        System.out.println("GetInputBidValue: " + inputViewState);
        try {
            log.info("ViewState: {}", inputViewState.getValues());

            var blockMap = inputViewState.getValues();
            if (!blockMap.containsKey("bid_value")) {
                throw new IllegalStateException("Brak block_id 'bid_value' w ViewState");
            }

            var actionMap = blockMap.get("bid_value");
            if (!actionMap.containsKey("bid_input")) {
                throw new IllegalStateException("Brak action_id 'bid_input' w ViewState.block[bid_value]");
            }

            String rawValue = actionMap.get("bid_input").getValue();
            log.info("Pobrana wartość z modala: {}", rawValue);

            String normalized = rawValue.replace(',', '.').trim();

            if (!normalized.matches("\\d+(\\.\\d{1,2})?")) {
                throw new IllegalArgumentException("Niepoprawny format kwoty: " + rawValue);
            }

            return new BigDecimal(normalized);
        } catch (Exception e) {
            log.error("Błąd przy pobieraniu wartości inputa z ViewState: {}", e.getMessage(), e);
            throw new IllegalStateException("Nie udało się pobrać wartości z modala: " + e.getMessage());
        }
    }


}