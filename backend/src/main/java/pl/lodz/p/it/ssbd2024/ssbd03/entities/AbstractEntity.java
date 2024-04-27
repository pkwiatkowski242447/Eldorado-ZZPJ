package pl.lodz.p.it.ssbd2024.ssbd03.entities;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import pl.lodz.p.it.ssbd2024.ssbd03.utils.consts.DatabaseConsts;

import java.util.UUID;

/**
 * Entity used as a basis for all Entities in the system.
 * Used to simplify the classes and unify matters related with id generation and version tracking.
 */
@MappedSuperclass
@Getter @ToString
public class AbstractEntity implements Signable {
    @Id
    @Column(name = DatabaseConsts.PK_COLUMN, columnDefinition = "UUID", unique = true, nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    @Column(name = DatabaseConsts.VERSION_COLUMN, nullable = false)
    @EqualsAndHashCode.Exclude
    private Long version;
}
