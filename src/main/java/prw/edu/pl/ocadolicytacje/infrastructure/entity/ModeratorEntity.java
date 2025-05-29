package prw.edu.pl.ocadolicytacje.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.GenerationType.*;

@Entity
@Table(name = "moderator")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModeratorEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "moderator_id")
    private Long moderator_id;
    @Column(name = "first_name", nullable = false)
    private String firstName;
    @Column(name = "last_name", nullable = false)
    private String lastName;

}
