package prw.edu.pl.ocadolicytacje.domain.service.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slack.api.bolt.context.builtin.ViewSubmissionContext;
import com.slack.api.bolt.handler.builtin.ViewSubmissionHandler;
import com.slack.api.bolt.request.builtin.ViewSubmissionRequest;
import com.slack.api.bolt.response.Response;
import com.slack.api.model.view.ViewState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import prw.edu.pl.ocadolicytacje.domain.model.Auction;
import prw.edu.pl.ocadolicytacje.domain.model.Bid;
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

    private final BidEntityRepository bidEntityRepository;
    private final ParticipantEntityRepository participantEntityRepository;
    private final AuctionEntityRepository auctionEntityRepository;
    private final AuctionRepository auctionRepository;

    @Override
    public Response apply(ViewSubmissionRequest req, ViewSubmissionContext ctx) {
        try {
            ViewState inputViewState = req.getPayload().getView().getState();
            JsonNode metaData = getRequestMetaData(req);

            BigDecimal inputBidValue = getInputBidValue(inputViewState);
            Long auctionId = metaData.get("auctionId").asLong();
            String userId = req.getPayload().getUser().getId();

            Auction auction = auctionRepository.findById(auctionId);

            if (!auction.getStatus()) {
                return ctx.ackWithErrors(Collections.singletonMap("bid_value", "Aukcja jest nieaktywna."));
            }

            final List<Bid> bids = auction.getBids();

            BigDecimal highestBid = bids.stream()
                    .max(Comparator.comparing(Bid::getBidValue))
                    .map(Bid::getBidValue)
                    .orElse(auction.getBasePrice());

            if (inputValueIsLowerThanActual(inputBidValue, highestBid)) {
                log.info("Provided price: {} is lower than actual highest price: {}", inputBidValue, highestBid);
                String errorMessage = "Provided price: %s is lower than actual highest price: %s".formatted(inputBidValue, highestBid);
                return ctx.ackWithErrors(Collections.singletonMap("bid", errorMessage));
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

            Bid previousHighestBid = bids.stream()
                    .max(Comparator.comparing(Bid::getBidValue))
                    .orElse(null);


            if (previousHighestBid != null && !previousHighestBid.getParticipantSlackId().equals(req.getPayload().getUser().getId())) {
                // osoba została przebita
                String outbidMessage = String.format(
                        "<@%s> Twoja oferta została przebita przez <@%s>. Nowa cena: %s zł",
                        previousHighestBid.getParticipantSlackId(),
                        req.getPayload().getUser().getId(),
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

            String value = actionMap.get("bid_input").getValue();
            log.info("Pobrana wartość z modala: {}", value);

            return new BigDecimal(value);
        } catch (Exception e) {
            log.error("Błąd przy pobieraniu wartości inputa z ViewState: {}", e.getMessage(), e);
            throw new IllegalStateException("Nie udało się pobrać wartości z modala: " + e.getMessage());
        }
    }


}