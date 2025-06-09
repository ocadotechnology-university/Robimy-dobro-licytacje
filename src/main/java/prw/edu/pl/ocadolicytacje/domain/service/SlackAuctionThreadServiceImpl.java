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
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import prw.edu.pl.ocadolicytacje.domain.model.Auction;
import prw.edu.pl.ocadolicytacje.domain.model.Bid;
import prw.edu.pl.ocadolicytacje.domain.model.Participant;
import prw.edu.pl.ocadolicytacje.domain.service.port.GoogleDriveService;
import prw.edu.pl.ocadolicytacje.domain.service.port.SlackAuctionThreadService;
import prw.edu.pl.ocadolicytacje.infrastructure.repository.ParticipantRepository;
import prw.edu.pl.ocadolicytacje.slack.SlackProperties;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static com.slack.api.model.block.element.BlockElements.asElements;
import static com.slack.api.model.block.element.BlockElements.button;

@Slf4j
@Service
//@RequiredArgsConstructor
public class SlackAuctionThreadServiceImpl implements SlackAuctionThreadService {

    private final SlackProperties slackProperties;
    private final ParticipantRepository participantRepository;
    private final Clock clock;
    private final MethodsClient slackClient;

    @Autowired
    private GoogleDriveService driveService;

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


    public List<LayoutBlock> buildAuctionBlocks(@NonNull final Auction auction, String channelId, Object threadTs) {
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
                        (auction.getSupplierFullName() != null
                                ? auction.getSupplierFullName()
                                : "Nieznana osoba wystawiająca przedmiot") + "\n" +
                        ":moneybag: *Cena wywoławcza:* " + auction.getBasePrice() + " zł"
        ))));

        BigDecimal highestBidValue = Optional.ofNullable(auction.getBids())
                .orElse(Collections.emptyList())
                .stream()
                .max(Comparator.comparing(Bid::getBidValue))
                .map(Bid::getBidValue)
                .orElse(auction.getBasePrice());

        blocks.add(section(s -> s.text(markdownText(
                ":arrow_up: *Aktualna najwyższa oferta:* " + highestBidValue + " zł"
        ))));

        String photoUrl = auction.getPhotoUrl();
        if (photoUrl != null && !photoUrl.isBlank()) {
            try {
                String id = extractGoogleDriveFileId(photoUrl);

                // a) jeśli URL wskazuje na plik => od razu robimy direct link
                if (!photoUrl.contains("/folders/")) {
                    String direct = "https://drive.google.com/uc?export=view&id=" + id;
                    blocks.add(image(i -> i.imageUrl(direct).altText("Zdjęcie aukcji")));
                }
                // b) jeśli to folder – pobierz pierwszy obraz przez Drive API
                else {
                    String imageId = driveService.getFirstImageId(id);
                    if (imageId != null) {
                        String direct = "https://drive.google.com/uc?export=view&id=" + imageId;
                        blocks.add(image(i -> i.imageUrl(direct).altText("Zdjęcie aukcji")));
                    } else {
                        log.warn("Folder {} nie zawiera publicznych obrazów; pomijam.", id);
                    }
                }
            } catch (IllegalArgumentException e) {
                log.warn("Nie udało się przekształcić linku do zdjęcia: {}", e.getMessage());
            }
        }

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


        ctx.client().chatUpdate(r -> r
                .channel(channelId)
                .ts(messageTs)
                .blocks(buildAuctionBlocks(auction, channelId, messageTs))
                .text("📢 Aukcja #" + auction.getAuctionId() + ": " + auction.getTitle())
        );

        ctx.client().chatPostMessage(r -> r
                .channel(channelId)
                .threadTs(messageTs)
                .text("✅ Aukcja została aktywowana. Można licytować!"));
    }

    public void updateSlackAuctionStatus(@NonNull final Auction auction) throws SlackApiException, IOException {
        final String channelId = slackProperties.getChannelId();
        final String messageTs = auction.getSlackMessageTs();

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


    private String extractGoogleDriveFileId(String url) {
        List<Pattern> patterns = List.of(
                Pattern.compile("/file/d/([a-zA-Z0-9_-]+)"),      // /file/d/FILE_ID
                Pattern.compile("/d/([a-zA-Z0-9_-]+)"),           // /d/FILE_ID
                Pattern.compile("[?&]id=([a-zA-Z0-9_-]+)"),       // ?id=FILE_ID
                Pattern.compile("/drive/folders/([a-zA-Z0-9_-]+)"), // /drive/folders/FOLDER_ID
                Pattern.compile("/folders/([a-zA-Z0-9_-]+)")      // /folders/FOLDER_ID (bez /drive)
        );

        for (Pattern p : patterns) {
            Matcher m = p.matcher(url);
            if (m.find()) {
                return m.group(1);
            }
        }
        throw new IllegalArgumentException("Nieprawidłowy link do Google Drive: " + url);
    }
    public void finishAuctionOnSlack(@NonNull Auction auction) throws IOException, SlackApiException {
        var channelId = slackProperties.getChannelId();
        var messageTs = auction.getSlackMessageTs();

        // 1️⃣ Wyłonienie zwycięzcy
        Bid highestBid = Optional.ofNullable(auction.getBids())
                .orElse(List.of())
                .stream()
                .max(Comparator.comparing(Bid::getBidValue))
                .orElse(null);

        // 2️⃣ Zmieniamy bloki – status NIEAKTYWNA + usuwamy przycisk
        List<LayoutBlock> newBlocks = buildAuctionBlocks(auction, channelId, messageTs)
                .stream()
                .filter(b -> !(b instanceof com.slack.api.model.block.ActionsBlock))   // usuń przycisk
                .map(b -> {
                    if (b instanceof com.slack.api.model.block.SectionBlock sb &&
                            sb.getText() != null &&
                            sb.getText().getText().contains("statusie")) {

                        var newText = sb.getText().getText()
                                .replace("AKTYWNA", "NIEAKTYWNA");
                        return section(s -> s.text(markdownText(newText)));
                    }
                    return b;
                })
                .toList();

        slackClient.chatUpdate(r -> r
                .channel(channelId)
                .ts(messageTs)
                .blocks(newBlocks)
                .text("🔒 Aukcja #" + auction.getAuctionId() + " zakończona")
        );

        // 3️⃣ Wiadomość we wątku
        if (highestBid != null) {
            slackClient.chatPostMessage(r -> r
                    .channel(channelId)
                    .threadTs(messageTs)
                    .text("🏆 Gratulacje <@" + highestBid.getParticipantSlackId() + ">! "
                            + "Wygrałeś aukcję #" + auction.getAuctionId()
                            + " za " + highestBid.getBidValue() + " zł."));
        } else {
            slackClient.chatPostMessage(r -> r
                    .channel(channelId)
                    .threadTs(messageTs)
                    .text("⏰ Aukcja zakończona bez złożonych ofert."));
        }
    }

    public void refreshPriceOnSlack(Auction auction) throws IOException, SlackApiException {
        String channelId = slackProperties.getChannelId();
        String ts = auction.getSlackMessageTs();

        List<LayoutBlock> newBlocks = buildAuctionBlocks(auction, channelId, ts);

        slackClient.chatUpdate(r -> r
                .channel(channelId)
                .ts(ts)
                .blocks(newBlocks)
                .text("📢 Aukcja #" + auction.getAuctionId() + ": " + auction.getTitle()));
    }

    public void sendBidToast(String slackUserId, BigDecimal bidValue, Auction auction)
            throws IOException, SlackApiException {

        slackClient.chatPostEphemeral(r -> r
                .channel(slackProperties.getChannelId())
                .user(slackUserId)
                .text(":white_check_mark: Twoja oferta *" + bidValue + " zł* "
                        + "dla aukcji *#" + auction.getAuctionId() + "* została zapisana."));
    }

}
