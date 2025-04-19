package prw.edu.pl.ocadolicytacje.domain.service.handler;

import com.slack.api.bolt.context.builtin.ActionContext;
import com.slack.api.bolt.handler.builtin.BlockActionHandler;
import com.slack.api.bolt.request.builtin.BlockActionRequest;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.block.InputBlock;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.PlainTextInputElement;
import com.slack.api.model.view.View;
import com.slack.api.model.view.ViewClose;
import com.slack.api.model.view.ViewSubmit;
import com.slack.api.model.view.ViewTitle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;

@Component
public class BidButtonHandler implements BlockActionHandler {

    @Override
    public Response apply(BlockActionRequest req, ActionContext ctx) throws SlackApiException, IOException {
        String triggerId = req.getPayload().getTriggerId();
        String channelId = req.getPayload().getContainer().getChannelId();
        String messageTs = req.getPayload().getContainer().getMessageTs();
        String auctionId = req.getPayload().getActions().get(0).getValue().split(":")[1];

        View modal = View.builder()
                .type("modal")
                .callbackId("bid_modal")
                .title(ViewTitle.builder().type("plain_text").text("Złóż ofertę").build())
                .submit(ViewSubmit.builder().type("plain_text").text("Licytuj").build())
                .close(ViewClose.builder().type("plain_text").text("Anuluj").build())
                .privateMetadata("{\"auctionId\":\"" + auctionId + "\",\"channelId\":\"" + channelId + "\",\"messageTs\":\"" + messageTs + "\"}")
                .blocks(Collections.singletonList(
                        InputBlock.builder()
                                .blockId("bid_value")
                                .label(PlainTextObject.builder().text("Wpisz ofertę (zł)").build())
                                .element(PlainTextInputElement.builder()
                                        .actionId("bid_input")
                                        .placeholder(PlainTextObject.builder().text("np. 150.00").build())
                                        .build())
                                .build()
                ))
                .build();

        ctx.client().viewsOpen(r -> r.triggerId(triggerId).view(modal));
        return ctx.ack();
    }
}

