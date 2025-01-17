package pl.lodz.p.it.ssbd2024.ssbd03.entities.mop;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import pl.lodz.p.it.ssbd2024.ssbd03.aspects.logging.LoggerInterceptor;
import pl.lodz.p.it.ssbd2024.ssbd03.entities.mok.Account;
import pl.lodz.p.it.ssbd2024.ssbd03.utils.consts.DatabaseConsts;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity containing all information about any modification made to the parking
 * object - such as the state of all the fields in the object, user that
 * made the change and time, which the change took place at.
 */
@Entity
@Table(
        name = DatabaseConsts.PARKING_HIST_TABLE,
        uniqueConstraints = @UniqueConstraint(columnNames = {DatabaseConsts.PARKING_HIST_ID_COLUMN, DatabaseConsts.PARKING_HIST_VERSION_COLUMN}),
        indexes = {
                @Index(name = DatabaseConsts.PARKING_HIST_ACCOUNT_ID_INDEX, columnList = DatabaseConsts.PARKING_HIST_MODIFIED_BY_COLUMN)
        }
)
@LoggerInterceptor
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
@NamedQueries({
        @NamedQuery(
                name = "ParkingHistoryData.findByParkingId",
                query = """
                        SELECT p FROM ParkingHistoryData p
                        WHERE p.id = :id
                        ORDER BY p.modificationTime DESC
                        """
        ),
        @NamedQuery(
                name = "ParkingHistoryData.checkIfEntityExists",
                query = """
                        SELECT 1 FROM ParkingHistoryData p
                        WHERE p.id = :id AND p.version = :version
                        """
        )
})
public class ParkingHistoryData {

    @Id
    @Column(name = DatabaseConsts.PARKING_HIST_ID_COLUMN, columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @Id
    @Column(name = DatabaseConsts.PARKING_HIST_VERSION_COLUMN, updatable = false, nullable = false)
    private Long version;

    @Column(name = DatabaseConsts.PARKING_HIST_CITY_COLUMN, nullable = false, updatable = false)
    private String city;

    @Column(name = DatabaseConsts.PARKING_HIST_STREET_COLUMN, nullable = false, updatable = false)
    private String street;

    @Column(name = DatabaseConsts.PARKING_HIST_ZIP_CODE_COLUMN, nullable = false, updatable = false, length = 6)
    private String zipCode;

    @Column(name = DatabaseConsts.PARKING_HIST_STRATEGY_COLUMN, nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private Parking.SectorDeterminationStrategy strategy;

    @Column(name = DatabaseConsts.PARKING_HIST_MODIFICATION_TIME_COLUMN, nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime modificationTime;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = DatabaseConsts.PARKING_HIST_MODIFIED_BY_COLUMN,
            referencedColumnName = DatabaseConsts.PK_COLUMN,
            foreignKey = @ForeignKey(name = DatabaseConsts.PARKING_HIST_ACCOUNT_ID_FK),
            updatable = false
    )
    private Account modifiedBy;

    @PrePersist
    @PreUpdate
    @PreRemove
    private void setModificationTime() {
        this.modificationTime = LocalDateTime.now();
    }

    /**
     * Constructor of ParkingHistoryData entity object, used to extract data from the current version
     * of the parking.
     *
     * @param parking Parking, which current version is being persisted to the history table.
     * @param modifiedBy Account of the user, who modified the parking entity.
     */
    public ParkingHistoryData(Parking parking, Account modifiedBy) {
        this.id = parking.getId();
        this.version = parking.getVersion();
        this.city = parking.getAddress().getCity();
        this.street = parking.getAddress().getStreet();
        this.zipCode = parking.getAddress().getZipCode();
        this.strategy = parking.getSectorStrategy();
        this.modifiedBy = modifiedBy;
    }

    /**
     * Custom toString() method implementation, defined in order
     * to avoid potential leaks of business data to the logs.
     * @return String representation of the ParkingHistoryData
     * object without any sensitive data.
     */
    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("Id", id)
                .append("Version", version)
                .toString();
    }
}
