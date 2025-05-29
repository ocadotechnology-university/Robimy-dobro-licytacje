package prw.edu.pl.ocadolicytacje.domain.service;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import prw.edu.pl.ocadolicytacje.slack.SlackProperties;

@Configuration
public class SlackConfig {

    @Bean
    public MethodsClient methodsClient(SlackProperties slackProperties) {
        return Slack.getInstance().methods(slackProperties.getBotToken());
    }
}

