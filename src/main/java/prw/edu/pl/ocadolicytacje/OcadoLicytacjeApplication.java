package prw.edu.pl.ocadolicytacje;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;
import prw.edu.pl.ocadolicytacje.slack.SlackProperties;
import prw.edu.pl.ocadolicytacje.slack.YamlPropertySourceFactory;

import java.util.Locale;

@SpringBootApplication
@EnableScheduling
@ServletComponentScan
@EnableConfigurationProperties(SlackProperties.class)
@PropertySource(value = "classpath:application-secrets.yml", factory = YamlPropertySourceFactory.class)
public class OcadoLicytacjeApplication {

    public static void main(String[] args) {
        Locale.setDefault(new Locale("pl", "PL"));
        SpringApplication.run(OcadoLicytacjeApplication.class, args);
    }

}
