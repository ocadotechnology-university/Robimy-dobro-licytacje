package prw.edu.pl.ocadolicytacje.domain.service.port;

import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.methods.SlackApiException;

import java.io.IOException;

public interface AuctionActivationService {
    /**
     * @param ctx
     */
    void activateAuctionManually(SlashCommandContext ctx) throws SlackApiException, IOException;
    void activateScheduledAuction() throws SlackApiException, IOException;
}
