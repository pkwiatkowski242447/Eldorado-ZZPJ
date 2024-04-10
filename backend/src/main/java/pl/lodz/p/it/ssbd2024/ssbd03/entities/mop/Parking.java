package pl.lodz.p.it.ssbd2024.ssbd03.entities.mop;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import pl.lodz.p.it.ssbd2024.ssbd03.entities.AbstractEntity;
import pl.lodz.p.it.ssbd2024.ssbd03.utils.consts.DatabaseConsts;
import pl.lodz.p.it.ssbd2024.ssbd03.utils.consts.mop.ParkingConsts;
import pl.lodz.p.it.ssbd2024.ssbd03.utils.messages.mop.ParkingMessages;

import static pl.lodz.p.it.ssbd2024.ssbd03.entities.mop.Sector.SectorType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(
        name = DatabaseConsts.PARKING_TABLE,
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {DatabaseConsts.PARKING_CITY_COLUMN, DatabaseConsts.PARKING_ZIP_CODE_COLUMN, DatabaseConsts.PARKING_STREET_COLUMN})
        }
)
@ToString(callSuper = true)
@NoArgsConstructor
@NamedQueries({
        @NamedQuery(
                name = "Parking.findAll",
                query = """
                        SELECT s.parking FROM Sector s
                        WHERE (:showOnlyActive != true OR s.weight>0)
                        GROUP BY s.parking
                        ORDER BY s.parking.address.city, s.parking.address.city"""
        ),
        @NamedQuery(
          name = "Parking.findBySectorTypes",
          query = """
                  SELECT s.parking FROM Sector s
                  WHERE s.type IN :sectorTypes AND (:showOnlyActive != true OR s.weight>0)
                  GROUP BY s.parking
                  ORDER BY s.parking.address.city, s.parking.address.city"""
        ),
        @NamedQuery(
                name = "Parking.findWithAvailablePlaces",
                query = """
                        SELECT s.parking FROM Sector s 
                        WHERE s.availablePlaces != 0 AND (:showOnlyActive != true OR s.weight>0) 
                        GROUP BY s.parking 
                        ORDER BY s.parking.address.city, s.parking.address.city"""
        )
})

@Getter
public class Parking extends AbstractEntity {

    @NotNull(message = ParkingMessages.ADDRESS_NULL)
    @Embedded
    @Setter
    private Address address;

    @NotNull(message = ParkingMessages.LIST_OF_SECTORS_NULL)
    @Size(min = ParkingConsts.LIST_OF_SECTORS_MIN_SIZE, message = ParkingMessages.LIST_OF_SECTORS_EMPTY)
    @Size(min = ParkingConsts.LIST_OF_SECTORS_MAX_SIZE, message = ParkingMessages.LIST_OF_SECTORS_FULL)
    @OneToMany(mappedBy = DatabaseConsts.PARKING_TABLE, cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    @ToString.Exclude
    private List<Sector> sectors = new ArrayList<>();

    public void addSector(String name, SectorType type, Integer maxPlaces, Integer weight) {
        sectors.add(new Sector(this,name,type,maxPlaces,weight));
    }

    public void deleteSector(String sectorName) {
        //Replace sector list with the list without the specified sector
        sectors = sectors.stream().filter(sector -> !sector.getName().equals(sectorName)).collect(Collectors.toList());
    }

    public void assignClient() {
        ///TODO implement
    }

    public void changeSectorWeight(String sectorName, Integer newWeight) {
        ///TODO implement
    }
}