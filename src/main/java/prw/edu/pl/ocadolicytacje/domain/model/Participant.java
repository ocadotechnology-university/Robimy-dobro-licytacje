package prw.edu.pl.ocadolicytacje.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class Participant {

    private Long participantId;
    private String firstName;
    private String lastName;
    private String city;
    private String slackUserId;

}
