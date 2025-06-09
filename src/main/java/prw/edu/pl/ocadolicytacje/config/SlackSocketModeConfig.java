package prw.edu.pl.ocadolicytacje.config;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.jakarta_socket_mode.SocketModeApp;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.files.FilesUploadRequest;
import com.slack.api.methods.response.files.FilesUploadResponse;
import com.slack.api.methods.response.files.FilesUploadV2Response;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import prw.edu.pl.ocadolicytacje.domain.service.AuctionActivationServiceImpl;
import prw.edu.pl.ocadolicytacje.domain.service.AuctionEndServiceImpl;
import prw.edu.pl.ocadolicytacje.domain.service.CsvReportServiceImpl;
import prw.edu.pl.ocadolicytacje.domain.service.GeneralSlackAuctionServiceImpl;
import prw.edu.pl.ocadolicytacje.domain.service.handler.BidButtonHandler;
import prw.edu.pl.ocadolicytacje.domain.service.handler.BidModalHandler;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;


@Configuration
@Slf4j
public class SlackSocketModeConfig {
    public static final String START_AUKCJE_COMMAND = "/start_aukcje";

    private final String botToken;
    private final String appToken;

    private final GeneralSlackAuctionServiceImpl generalSlackAuctionServiceImpl;
    private final AuctionActivationServiceImpl auctionActivationServiceImpl;
    private final AuctionEndServiceImpl auctionEndServiceImpl;
    private final BidButtonHandler bidButtonHandler;
    private final BidModalHandler bidModalHandler;
    private final CsvReportServiceImpl csvReportService;

    public SlackSocketModeConfig(
            @Value("${slack.bot-token}") String botToken,
            @Value("${slack.app-token}") String appToken,
            @NonNull GeneralSlackAuctionServiceImpl generalSlackAuctionServiceImpl,
            @NonNull AuctionActivationServiceImpl auctionActivationServiceImpl,
            @NonNull AuctionEndServiceImpl auctionEndServiceImpl,
            @NonNull BidButtonHandler bidButtonHandler,
            @NonNull BidModalHandler bidModalHandler,
            @NonNull CsvReportServiceImpl csvReportService
    ) {
        this.botToken = botToken;
        this.appToken = appToken;
        this.generalSlackAuctionServiceImpl = generalSlackAuctionServiceImpl;
        this.auctionActivationServiceImpl = auctionActivationServiceImpl;
        this.auctionEndServiceImpl = auctionEndServiceImpl;
        this.bidButtonHandler = bidButtonHandler;
        this.bidModalHandler = bidModalHandler;
        this.csvReportService = csvReportService;
    }

    @Bean
    public SocketModeApp socketModeApp(App app) throws Exception {
        try {
            SocketModeApp socketModeApp = new SocketModeApp(appToken, app);
            socketModeApp.startAsync();
            return socketModeApp;
        } catch (Exception e) {
            log.error("Nie udało się uruchomić SocketModeApp", e);
            throw new IllegalStateException(e);
        }
    }

    @Bean
    public App slackApp() {
        AppConfig config = AppConfig.builder()
                .singleTeamBotToken(botToken)
                .build();

        App app = new App(config);
        app.blockAction("bid_button", bidButtonHandler);
        app.viewSubmission("bid_modal", bidModalHandler);


        app.command(START_AUKCJE_COMMAND, (req, ctx) -> {
            String text = req.getPayload().getText();
            log.info("Odebrany tekst z komendy: '{}'", text);
            try {
                final LocalDate date = LocalDate.parse(text.trim());
                generalSlackAuctionServiceImpl.createAuctionThreadAndPostOnChannel(date, ctx);
                auctionActivationServiceImpl.activateScheduledAuction();
                auctionEndServiceImpl.endAuctionsManual(ctx);

                return ctx.ack("Aukcje dla " + date + " zostały przygotowane");
            } catch (DateTimeParseException e) {
                log.info("Podano datę rozpoczęcia w błędnym formacie. Prawidłowy format to: YYYY-MM-DD", e);
                return ctx.ack("Podano datę rozpoczęcia w błędnym formacie. Prawidłowy format to: YYYY-MM-DD");
            }
            catch (Exception e) {
                log.error("Błąd podczas tworzenia aukcji", e);
                return ctx.ack("❌ Wystąpił błąd podczas tworzenia aukcji: " + e.getMessage());
            }
        });

        app.command("/generuj_raport", (req, ctx) -> {
            String text = req.getPayload().getText().trim();
            if (text.isEmpty()) {
                return ctx.ack("❌ Musisz podać datę w formacie YYYY-MM-DD, np. /generuj_raport 2025-06-08");
            }
            try {
                LocalDate date = LocalDate.parse(text);
                ctx.ack("📢 Generowanie raportu aukcji zakończonych w dniu: " + date);

                List<String> messages = csvReportService.generateAuctionSummaryMessagesForDate(date);
                String channelId = req.getPayload().getChannelId();

                for (String message : messages) {
                    ctx.client().chatPostMessage(r -> r
                            .channel(channelId)
                            .text(message));
                }

                return ctx.ack("✅ Raport został wygenerowany i opublikowany w postaci wiadomości.");
            } catch (DateTimeParseException e) {
                log.info("Podano datę w błędnym formacie: {}", text, e);
                return ctx.ack("❌ Nieprawidłowy format daty. Użyj formatu YYYY-MM-DD.");
            } catch (Exception e) {
                log.error("Błąd podczas generowania wiadomości", e);
                return ctx.ack("❌ Wystąpił błąd: " + e.getMessage());
            }
        });




        return app;
    }


    //TODO - DODANIE KOMENDY, do generowania raportu


}