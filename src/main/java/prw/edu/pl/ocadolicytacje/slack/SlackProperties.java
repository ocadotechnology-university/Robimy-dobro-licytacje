package prw.edu.pl.ocadolicytacje.slack;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "slack")
@Getter
@Setter
public class SlackProperties {
    private String botToken;
    private String channelId;
    private String signingSecret;
}