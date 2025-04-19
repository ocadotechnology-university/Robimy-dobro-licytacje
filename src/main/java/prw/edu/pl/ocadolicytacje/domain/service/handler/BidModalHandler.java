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
import prw.edu.pl.ocadolicytacje.infrastructure.repository.AuctionRepository;
import prw.edu.pl.ocadolicytacje.infrastructure.repository.BidRepository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class BidModalHandler implements ViewSubmissionHandler {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;

    @Override
    public Response apply(ViewSubmissionRequest req, ViewSubmissionContext ctx) {
        try {
            ViewState inputViewState = req.getPayload().getView().getState();
            JsonNode metaData = getRequestMetaData(req);

            BigDecimal inputBidValue = getInputBidValue(inputViewState);
            Long auctionId = metaData.get("auctionId").asLong();
            String user = req.getPayload().getUser().getUsername();

            Auction auction = auctionRepository.findById(auctionId);

            if (!auction.getStatus()) {
                return ctx.ackWithErrors(Collections.singletonMap("bid_value", "Aukcja jest nieaktywna."));
            }

            final List<Bid> bids = auction.getBids();
            BigDecimal highestBid = bids.stream()
                    .max(Comparator.comparing(Bid::getBidValue))
                    .map(Bid::getBidValue)
                    .get();

            if (highestBid == null) {
                highestBid = auction.getBasePrice();
            }

            if (inputValueIsLowerThanActual(inputBidValue, highestBid)) {
                log.info("Provided price: {} is lower than actual highest price: {}", inputBidValue, highestBid);
                String errorMessage = "Provided price: %s is lower than actual highest price: %s".formatted(inputBidValue, highestBid);
                return ctx.ackWithErrors(Collections.singletonMap("bid", errorMessage));
            }

            Bid bid = buildBid(auctionId, user, inputBidValue);
            bidRepository.save(bid);


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
        String value = inputViewState.getValues()
                .get("bid_value")
                .get("bid_input")
                .getValue();
        return new BigDecimal(value);
    }
}