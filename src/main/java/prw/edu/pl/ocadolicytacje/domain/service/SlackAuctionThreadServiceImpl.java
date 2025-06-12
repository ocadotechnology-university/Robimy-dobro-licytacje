package prw.edu.pl.ocadolicytacje.domain.service;

import com.slack.api.bolt.context.Context;
import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.chat.ChatUpdateResponse;
import com.slack.api.model.block.ContextBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import prw.edu.pl.ocadolicytacje.domain.model.Auction;
import prw.edu.pl.ocadolicytacje.domain.model.Bid;
import prw.edu.pl.ocadolicytacje.domain.model.Participant;
import prw.edu.pl.ocadolicytacje.domain.service.port.SlackAuctionThreadService;
import prw.edu.pl.ocadolicytacje.infrastructure.repository.ParticipantRepository;
import prw.edu.pl.ocadolicytacje.slack.SlackProperties;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static com.slack.api.model.block.element.BlockElements.asElements;
import static com.slack.api.model.block.element.BlockElements.button;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SlackAuctionThreadServiceImpl implements SlackAuctionThreadService {

    private final SlackProperties slackProperties;
    private final ParticipantRepository participantRepository;
    private final Clock clock;
    private final MethodsClient slackClient;

    public SlackAuctionThreadServiceImpl(
            SlackProperties slackProperties,
            ParticipantRepository participantRepository,
            Clock clock,
            MethodsClient slackClient) {
        this.slackProperties = slackProperties;
        this.participantRepository = participantRepository;
        this.clock = clock;
        this.slackClient = slackClient;
    }

    @Nullable
    private static Bid getHighestBid(List<Bid> bids) {
        Bid highestBid = bids.stream()
                .max(Comparator.comparing(Bid::getBidValue))
                .orElse(null);
        return highestBid;
    }

    public String postAuctionToSlack(@NonNull final Auction auction, @NonNull final Context ctx, String channelId) throws IOException, SlackApiException {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("Kanał Slack nie może być pusty!");
        }

        log.info("Wysyłanie aukcji do Slacka, channelId={}, auctionId={}", channelId, auction.getAuctionId());

        List<LayoutBlock> blocks = buildAuctionBlocks(auction, channelId, null);
        if (blocks == null || blocks.isEmpty()) {
            throw new IllegalStateException("Bloki wiadomości są puste – nie można wysłać wiadomości.");
        }

        log.debug("Bloki wiadomości: {}", blocks);

        ChatPostMessageResponse mainMessage = ctx.client().chatPostMessage(r -> r
                .channel(channelId)
                .text("📢 Aukcja #" + auction.getAuctionId() + ": " + auction.getTitle())
                .blocks(blocks)
        );

        if (!mainMessage.isOk()) {
            log.error("Błąd Slack API podczas chatPostMessage: {}", mainMessage.getError());
            throw new RuntimeException("Błąd Slack API: " + mainMessage.getError());
        }

        String threadTs = mainMessage.getTs();

        ChatPostMessageResponse threadMessage = ctx.client().chatPostMessage(r -> r
                .channel(channelId)
                .threadTs(threadTs)
                .text("Wątek licytacyjny dla aukcji #" + auction.getAuctionId())
        );

        if (!threadMessage.isOk()) {
            log.warn("Błąd przy wysyłaniu wiadomości do wątku: {}", threadMessage.getError());
        }

        ChatUpdateResponse updateResponse = ctx.client().chatUpdate(r -> r
                .channel(channelId)
                .ts(threadTs)
                .blocks(buildAuctionBlocks(auction, channelId, threadTs))
                .text("📢 Aukcja #" + auction.getAuctionId() + ": " + auction.getTitle())
        );

        if (!updateResponse.isOk()) {
            log.warn("Błąd przy aktualizacji wiadomości: {}", updateResponse.getError());
        }

        return threadTs;
    }


    private List<LayoutBlock> buildAuctionBlocks(@NonNull final Auction auction, String channelId, Object threadTs) {
        List<LayoutBlock> blocks = new ArrayList<>();

        blocks.add(section(s -> s.text(markdownText(
                "*AUKCJA #" + auction.getAuctionId() + "* – *" + auction.getTitle() + "*\n" +
                        "Aukcja jest w statusie: " + (auction.getStatus() ? "AKTYWNA" : "NIEAKTYWNA")
        ))));

        blocks.add(section(s -> s.text(markdownText(
                ":city_sunrise: *Miasto:* " + auction.getCity() + "\n" +
                        ":page_facing_up: *Opis:* " + auction.getDescription() + "\n" +
                        ":hourglass_flowing_sand: *Start:* " + auction.getStartDateTime() + "\n" +
                        ":stopwatch: *Koniec:* " + auction.getEndDateTime() + "\n" +
                        ":bust_in_silhouette: *Wystawiający:* " +
                        (auction.getSupplierEntity() != null
                                ? auction.getSupplierEntity().getFirstName() + " " + auction.getSupplierEntity().getLastName()
                                : "_brak danych_") + "\n" +

                        ":moneybag: *Cena wywoławcza:* " + auction.getBasePrice() + " zł"
        ))));

//        blocks.add(image(i -> i.imageUrl(auction.getPhotoUrl()).altText("Zdjęcie aukcji")));

        blocks.add(buildContextBlock(channelId, threadTs));

        if (auction.getStatus()) {
            blocks.add(actions(a -> a.elements(asElements(
                    button(b -> b
                            .style("primary")
                            .text(plainText("Licytuj"))
                            .actionId("bid_button")
                            .value("bid:" + auction.getAuctionId()))
            ))));
        }

        return blocks;
    }

    private ContextBlock buildContextBlock(String channelId, Object threadTs) {
        String text = (threadTs != null && channelId != null)
                ? "<https://slack.com/app_redirect?channel=" + channelId + "&thread_ts=" + threadTs + "|Przejdź do wątku licytacji>"
                : "_Wątek zostanie utworzony po opublikowaniu_";

        MarkdownTextObject markdown = markdownText(text);

        return ContextBlock.builder()
                .elements(List.of(markdown))
                .build();
    }

    public void updateSlackAuctionStatus(SlashCommandContext ctx, @NonNull final Auction auction) throws SlackApiException, IOException {
        final String channelId = slackProperties.getChannelId();
        final String messageTs = auction.getSlackMessageTs();


        if (auction.getStatus()) {
            slackClient.chatUpdate(r -> r
                    .channel(channelId)
                    .ts(messageTs)
                    .blocks(buildAuctionBlocks(auction, channelId, messageTs))
                    .text("📢 Aukcja #" + auction.getAuctionId() + ": " + auction.getTitle())
            );

            slackClient.chatPostMessage(r -> r
                    .channel(channelId)
                    .threadTs(messageTs)
                    .text("✅ Aukcja została aktywowana. Można licytować!"));
        }
        else if (!auction.getStatus()) {
            // Aukcja nieaktywna - bez przycisku (buildAuctionBlocks zwraca bloki bez przycisku)
            slackClient.chatUpdate(r -> r
                    .channel(channelId)
                    .ts(messageTs)
                    .blocks(buildAuctionBlocks(auction, channelId, messageTs))  // bez przycisku!
                    .text("📢 Aukcja #" + auction.getAuctionId() + ": " + auction.getTitle() + " (nieaktywna)")
            );

            slackClient.chatPostMessage(r -> r
                    .channel(channelId)
                    .threadTs(messageTs)
                    .text("❌ Aukcja została dezaktywowana. Licytacje są zamknięte."));
        }
    }

    public void updateSlackAuctionStatus(@NonNull final Auction auction) throws SlackApiException, IOException {
        final String channelId = slackProperties.getChannelId();
        final String messageTs = auction.getSlackMessageTs();

        if (auction.getStatus()) {
            slackClient.chatUpdate(r -> r
                    .channel(channelId)
                    .ts(messageTs)
                    .blocks(buildAuctionBlocks(auction, channelId, messageTs))
                    .text("📢 Aukcja #" + auction.getAuctionId() + ": " + auction.getTitle())
            );

            slackClient.chatPostMessage(r -> r
                    .channel(channelId)
                    .threadTs(messageTs)
                    .text("✅ Aukcja została aktywowana. Można licytować!"));
        }
        else if (!auction.getStatus()) {
            // Aukcja nieaktywna - bez przycisku (buildAuctionBlocks zwraca bloki bez przycisku)
            slackClient.chatUpdate(r -> r
                    .channel(channelId)
                    .ts(messageTs)
                    .blocks(buildAuctionBlocks(auction, channelId, messageTs))  // bez przycisku!
                    .text("📢 Aukcja #" + auction.getAuctionId() + ": " + auction.getTitle() + " (nieaktywna)")
            );

            slackClient.chatPostMessage(r -> r
                    .channel(channelId)
                    .threadTs(messageTs)
                    .text("❌ Aukcja została dezaktywowana. Licytacje są zamknięte."));
        }
    }

    public void sendPrivateWinMessage(Participant participant, Auction auction, Context ctx) throws IOException, SlackApiException {
        if (participant.getSlackUserId() != null) {
            ctx.client().chatPostMessage(r -> r
                    .channel(participant.getSlackUserId())
                    .text("🎉 Gratulacje! Wygrałeś aukcję #" + auction.getAuctionId() + " za " + auction.getWinPrice() + " zł. Skontaktuj się z wystawiającym, aby ustalić szczegóły przekazania."));
        }
    }

    public void sendSummaryToChannel(@NonNull final List<Auction> auctions, @NonNull final Context ctx) throws IOException, SlackApiException {
        final StringBuilder sb = new StringBuilder();
        sb.append("Podsumowanie dzisiejszych licytacji:\n\n");

        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        final String endDate = LocalDate.now(clock).format(formatter);

        for (Auction auction : auctions) {
            List<Bid> bids = Optional.ofNullable(auction.getBids()).orElse(Collections.emptyList());

            if (!bids.isEmpty()) {
                final Bid highestBid = bids.stream()
                        .max(Comparator.comparing(Bid::getBidValue))
                        .orElse(null);

                if (highestBid != null) {
                    Participant participant = participantRepository.findBySlackUserId(highestBid.getParticipantSlackId());

                    if (participant != null) {
                        sb.append("AUKCJA *").append(auction.getAuctionId())
                                .append("* - zwycięzca: *")
                                .append(participant.getFirstName()).append(" ").append(participant.getLastName())
                                .append("* - *").append(highestBid.getBidValue()).append("* zł\n\n");
                    } else {
                        sb.append("AUKCJA *").append(auction.getAuctionId())
                                .append("* - zwycięzca: *nieznany uczestnik* - *").append(highestBid.getBidValue()).append("* zł\n\n");
                    }
                } else {
                    sb.append("AUKCJA *").append(auction.getAuctionId())
                            .append("* - brak propozycji\n\n");
                }
            } else {
                sb.append("AUKCJA *").append(auction.getAuctionId())
                        .append("* - brak propozycji\n\n");
            }
            sb.append("Wszystkim zwycięzcom gratulujemy 👏 🎉\n\n")
                    .append("Kwotę należy wpłacić na konto:\n\n")
                    .append("*PKO BP SA 12 3456 7890 1234 5678 9012 3456*\n\n")
                    .append("tytuł: Ocado RobimyDobro ").append(LocalDate.now(clock).getYear())
                    .append(" - [data wystawienia aukcji] / [numer aukcji]\n\n")
                    .append("Ostateczny termin wpłaty to *").append(endDate).append("* - dzień zakończenia zbiórki.\n\n")
                    .append("W celu odebrania wygranej proszę o bezpośredni kontakt z wystawiającym aukcję.\n\n")
                    .append("Dziękujemy bardzo za moderację wystawionych aukcji.");
        }
        ctx.client().chatPostMessage(r -> r
                .channel(slackProperties.getChannelId())
                .text(sb.toString()));
    }


}
