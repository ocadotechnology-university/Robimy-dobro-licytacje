package prw.edu.pl.ocadolicytacje.domain.service;

import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatDeleteResponse;
import com.slack.api.methods.response.conversations.ConversationsRepliesResponse;
import com.slack.api.model.Message;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class SlackThreadCleaner {

    public void deleteSlackThread(String channelId, String threadTs, String botToken) throws IOException, SlackApiException {
        Slack slack = Slack.getInstance();

        // 1. Pobierz wszystkie wiadomości z wątku
        ConversationsRepliesResponse replies = slack.methods(botToken).conversationsReplies(r -> r
                .channel(channelId)
                .ts(threadTs)
        );

        List<Message> messages = replies.getMessages();

        // 2. Usuń wszystkie wiadomości w wątku (od ostatniej do pierwszej, na wszelki wypadek)
        for (int i = 5/*messages.size() - 1*/; i >= 0; i--) {
            Message msg = messages.get(i);
            ChatDeleteResponse deleteResp = slack.methods(botToken).chatDelete(r -> r
                    .channel(channelId)
                    .ts(msg.getTs())
            );
            if (!deleteResp.isOk()) {
                System.err.println("Nie udało się usunąć wiadomości: " + msg.getTs() + " - " + deleteResp.getError());
            } else {
                System.out.println("Usunięto wiadomość: " + msg.getTs());
            }
        }
    }
}

