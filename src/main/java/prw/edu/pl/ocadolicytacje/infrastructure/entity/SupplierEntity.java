package prw.edu.pl.ocadolicytacje.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "supplier")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "supplier_id")
    private Long supplier_id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "city")
    private String city;
}
