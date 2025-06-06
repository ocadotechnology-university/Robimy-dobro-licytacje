package prw.edu.pl.ocadolicytacje.config;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.jakarta_socket_mode.SocketModeApp;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.files.FilesUploadRequest;
import com.slack.api.methods.response.files.FilesUploadResponse;
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
            try {
                LocalDate date = LocalDate.parse(text);
                ctx.ack("📄 Generowanie raportu CSV dla daty: " + date);

                File csvFile = csvReportService.generateCsvReportForDate(date);

                FilesUploadRequest request = FilesUploadRequest.builder()
                        .channels(Collections.singletonList(req.getPayload().getChannelId()))
                        .file(csvFile)
                        .filename(csvFile.getName())
                        .title("Raport z dnia " + date)
                        .build();

                FilesUploadResponse response = ctx.client().filesUpload(request);
                if (!response.isOk()) {
                    return ctx.ack(" Błąd podczas przesyłania pliku: " + response.getError());
                }

                return ctx.ack("✅ Raport CSV został wygenerowany i przesłany.");
            } catch (DateTimeParseException e) {
                log.info("Podano datę w błędnym formacie: {}", text, e);
                return ctx.ack("❌ Nieprawidłowy format daty. Użyj formatu YYYY-MM-DD.");
            } catch (IOException | SlackApiException e) {
                log.error("Błąd podczas generowania lub przesyłania raportu", e);
                return ctx.ack("❌ Wystąpił błąd podczas generowania lub przesyłania raportu.");
            }
        });


        return app;
    }


    //TODO - DODANIE KOMENDY, do generowania raportu


}