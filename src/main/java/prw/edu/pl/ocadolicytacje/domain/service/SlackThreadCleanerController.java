package prw.edu.pl.ocadolicytacje.domain.service;


import com.slack.api.methods.SlackApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/slack/commands")
class SlackCommandController {

    private final SlackThreadCleaner cleaner;

    @Value("${slack.bot-token}")
    private String slackBotToken;

    public SlackCommandController(SlackThreadCleaner cleaner) {
        this.cleaner = cleaner;
    }

    @PostMapping("/delete_all_threads")
    public String deleteThread(
            @RequestParam("text") String text,
            @RequestParam("user_name") String userName,
            @RequestParam("channel_id") String channelId,
            HttpServletRequest request
    ) {
        try {
            String[] parts = text.split("\\s+");
            if (parts.length < 1) {
                return "❌ Użycie: `/usun_watek <thread_ts>`";
            }

            String threadTs = parts[0];

            cleaner.deleteSlackThread(channelId, threadTs, slackBotToken);
            return "✅ Wątek `" + threadTs + "` został usunięty przez @" + userName;
        } catch (SlackApiException | IOException e) {
            e.printStackTrace();
            return "❌ Błąd podczas usuwania wątku: " + e.getMessage();
        }
    }
}

