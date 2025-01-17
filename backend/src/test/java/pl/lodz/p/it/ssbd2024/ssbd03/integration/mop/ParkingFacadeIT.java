package pl.lodz.p.it.ssbd2024.ssbd03.integration.mop;

import com.atomikos.jdbc.AtomikosDataSourceBean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import pl.lodz.p.it.ssbd2024.ssbd03.TestcontainersConfig;
import pl.lodz.p.it.ssbd2024.ssbd03.config.security.consts.Authorities;
import pl.lodz.p.it.ssbd2024.ssbd03.config.webconfig.WebConfig;
import pl.lodz.p.it.ssbd2024.ssbd03.entities.mok.Client;
import pl.lodz.p.it.ssbd2024.ssbd03.entities.mop.Address;
import pl.lodz.p.it.ssbd2024.ssbd03.entities.mop.Parking;
import pl.lodz.p.it.ssbd2024.ssbd03.entities.mop.Reservation;
import pl.lodz.p.it.ssbd2024.ssbd03.entities.mop.Sector;
import pl.lodz.p.it.ssbd2024.ssbd03.exceptions.ApplicationBaseException;
import pl.lodz.p.it.ssbd2024.ssbd03.mop.facades.ParkingFacade;
import pl.lodz.p.it.ssbd2024.ssbd03.mop.facades.ReservationFacade;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = WebConfig.class)
public class ParkingFacadeIT extends TestcontainersConfig {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("jdbc.ssbd03.url", () -> String.format("jdbc:postgresql://localhost:%s/ssbd03", postgres.getFirstMappedPort()));
    }

    @AfterEach
    void teardown() {
        ((AtomikosDataSourceBean) webApplicationContext.getBean("dataSourceAdmin")).close();
        ((AtomikosDataSourceBean) webApplicationContext.getBean("dataSourceAuth")).close();
        ((AtomikosDataSourceBean) webApplicationContext.getBean("dataSourceMOP")).close();
        ((AtomikosDataSourceBean) webApplicationContext.getBean("dataSourceMOK")).close();
    }

    @Autowired
    ParkingFacade parkingFacade;
    @Autowired
    ReservationFacade reservationFacade;

    private Address address;
    private Parking parking;
    private Sector sector;
    private Reservation reservation;


    @BeforeEach
    public void setup() {
        address = new Address("Lodz", "90-000", "Pomorska");
        parking = new Parking(address, Parking.SectorDeterminationStrategy.LEAST_OCCUPIED);
        sector = new Sector(parking, "AA-01", Sector.SectorType.COVERED, 23, 11);
        reservation = new Reservation(sector, LocalDateTime.now());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRED)
    @WithMockUser(roles = {Authorities.ADD_PARKING})
    public void parkingFacadeCreateParkingTest() throws ApplicationBaseException {
        assertNotNull(parking);
        parkingFacade.create(parking);

        assertEquals("Lodz", parking.getAddress().getCity());
    }

//    @Test
//    @Transactional(propagation = Propagation.REQUIRED)
//    public void parkingFacadeFindParkingTest() throws ApplicationBaseException {
//        UUID uuid = UUID.fromString("96a36faa-f2a2-41b8-9c3c-b6bef04ce6d1");
//        Optional<Parking> retrievedParkingOptional = parkingFacade.find(uuid);
//        assertTrue(retrievedParkingOptional.isPresent());
//
//        Parking retrievedParking = retrievedParkingOptional.get();
//        assertNotNull(retrievedParking);
//    }

//    @Test
//    @Transactional(propagation = Propagation.REQUIRED)
//    public void parkingFacadeEditParkingTest() throws ApplicationBaseException {
//        Address addressNo1 = new Address("Tes", "Te", "Tes");
//        Parking parkingNo1 = new Parking(addressNo1);
//        parkingFacade.create(parkingNo1);
//
//        Address newAddress = new Address("New", "NewT", "NewT");
//        parkingNo1.setAddress(newAddress);
//
//        parkingFacade.edit(parkingNo1);
//
//        Parking editedParking = parkingFacade.find(parkingNo1.getId()).orElseThrow(NoSuchElementException::new);
//        assertEquals(newAddress, editedParking.getAddress());
//    }

    @Test
    @Transactional(propagation = Propagation.REQUIRED)
    @WithMockUser(roles = {Authorities.GET_PARKING, Authorities.ADD_PARKING})
    void parkingFacadeFindAndRefreshParkingTest() throws ApplicationBaseException {
        parkingFacade.create(parking);

        UUID parkingId = parking.getId();

        Optional<Parking> optionalParking = parkingFacade.findAndRefresh(parkingId);
        assertTrue(optionalParking.isPresent());

        Parking refreshedParking = optionalParking.get();
        assertNotNull(refreshedParking);
    }

//    @Test
//    @Transactional(propagation = Propagation.REQUIRED)
//    public void parkingFacadeCountParkingLotsTest() throws ApplicationBaseException {
//        parkingFacade.create(parking);
//        int parkingCount = parkingFacade.count();
//        assertEquals(2, parkingCount);
//    }

//    @Test
//    @Transactional(propagation = Propagation.REQUIRED)
//    public void parkingFacadeRemoveParkingTest() throws ApplicationBaseException {
//        parkingFacade.create(parking);
//        Optional<Parking> retrivedParkingOptional = parkingFacade.find(parking.getId());
//        assertTrue(retrivedParkingOptional.isPresent());
//
//        Parking retrievedParking = retrivedParkingOptional.get();
//        assertNotNull(retrievedParking);
//
//        parkingFacade.remove(parking);
//        Optional<Parking> deletedParkingOptional = parkingFacade.find(parking.getId());
//        assertTrue(deletedParkingOptional.isEmpty());
//    }

//    @Test
//    @Transactional(propagation = Propagation.REQUIRED)
//    public void parkingFacadeFindAllParkingLotsTest() throws Exception {
//        List<Parking> listOfParkingLots = parkingFacade.findAll();
//        assertNotNull(listOfParkingLots);
//        assertFalse(listOfParkingLots.isEmpty());
//
//        listOfParkingLots.add(parking);
//
//        assertEquals(2, listOfParkingLots.size());
//    }

    @Test
    @Transactional(propagation = Propagation.REQUIRED)
    @WithMockUser(roles = {Authorities.ADD_PARKING, Authorities.GET_ALL_PARKING})
    public void parkingFacadeFindAllParkingLotsWithPaginationTest() throws ApplicationBaseException {
        Address addressNo1 = new Address("BoatCity", "90-001", "Wodna");
        Address addressNo2 = new Address("BoatCity", "90-010", "Pomorska");
        Address addressNo3 = new Address("BoatCity", "90-100", "Kwiatowa");

        Parking parkingNo1 = new Parking(addressNo1, Parking.SectorDeterminationStrategy.LEAST_OCCUPIED);
        Parking parkingNo2 = new Parking(addressNo2, Parking.SectorDeterminationStrategy.LEAST_OCCUPIED);
        Parking parkingNo3 = new Parking(addressNo3, Parking.SectorDeterminationStrategy.LEAST_OCCUPIED);

        parkingFacade.create(parkingNo1);
        parkingFacade.create(parkingNo2);
        parkingFacade.create(parkingNo3);

        parkingNo1.addSector("SA-01", Sector.SectorType.COVERED, 100, 200);
        parkingNo1.addSector("SA-02", Sector.SectorType.COVERED, 20, 200);
        parkingNo2.addSector("SA-03", Sector.SectorType.UNCOVERED, 20, 200);
        parkingNo3.addSector("SA-04", Sector.SectorType.UNCOVERED, 20, 200);

        List<Parking> listOfParkingLots = parkingFacade.findAllWithPagination(0, 10, true);
        assertEquals(5, listOfParkingLots.size());
    }

//    @Test
//    @Transactional(propagation = Propagation.REQUIRED)
//    public void parkingFacadeFindParkingBySectorTypesTest() throws ApplicationBaseException {
//        Parking parkingNo1 = new Parking(new Address("fa", "fb", "cf"));
//        Parking parkingNo2 = new Parking(new Address("ab", "bbb", "cbf"));
//        Parking parkingNo3 = new Parking(new Address("a1", "b11", "cb11f"));
//        parkingFacade.create(parkingNo1);
//        parkingFacade.create(parkingNo2);
//        parkingFacade.create(parkingNo3);
//
//        parkingNo1.addSector("nnn", Sector.SectorType.COVERED, 100, 100);
//        parkingNo2.addSector("mmm", Sector.SectorType.COVERED, 100, 100);
//        parkingNo2.addSector("llll", Sector.SectorType.UNCOVERED, 100, 100);
//
//        parkingFacade.create(parkingNo1);
//
//        List<Sector.SectorType> sectorTypes = List.of(Sector.SectorType.COVERED);
//        List<Parking> listOfParkingLots = parkingFacade.findParkingBySectorTypes(sectorTypes, 0, 5, true);
//        assertEquals(2, listOfParkingLots.size());
//    }

    @Test
    @Transactional(propagation = Propagation.REQUIRED)
    @WithMockUser(roles = {Authorities.ADD_PARKING, Authorities.GET_ALL_SECTORS})
    public void parkingFacadeFindSectorsInParkingWithPaginationTest() throws ApplicationBaseException {
        Address addressNo1 = new Address("BoatCity", "90-000", "Pomorska");
        Parking parkingNo1 = new Parking(addressNo1, Parking.SectorDeterminationStrategy.LEAST_OCCUPIED);
        parkingNo1.addSector("SA-01", Sector.SectorType.COVERED, 100, 200);
        parkingNo1.addSector("SA-02", Sector.SectorType.UNCOVERED, 20, 200);
        parkingNo1.addSector("SA-03", Sector.SectorType.UNCOVERED, 20, 200);

        parkingFacade.create(parkingNo1);

        List<Sector> listOfSectors = parkingFacade.findSectorsInParkingWithPagination(parkingNo1.getId(), 0, 4, true);

        assertEquals(3, listOfSectors.size());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRED)
    @WithMockUser(roles = {Authorities.ADD_PARKING, Authorities.RESERVE_PARKING_PLACE})
    public void parkingFacadeFindSectorInParkingWithAvailablePlacesTest() throws ApplicationBaseException {
        Address addressNo1 = new Address("BoatCity", "90-000", "Pomorska");
        Parking parkingNo1 = new Parking(addressNo1, Parking.SectorDeterminationStrategy.LEAST_OCCUPIED);

        parkingFacade.create(parkingNo1);
        parkingNo1.addSector("SA-01", Sector.SectorType.COVERED, 100, 200);
        parkingNo1.addSector("SA-02", Sector.SectorType.UNCOVERED, 10, 200);
        parkingNo1.addSector("SA-03", Sector.SectorType.UNCOVERED, 10, 200);

        List<Sector> listOfSectorsNo1 = parkingNo1.getSectors();
        for (int i = 0; i < listOfSectorsNo1.size(); i++) {
            Sector sectorCopy = listOfSectorsNo1.get(i);
            sectorCopy.setOccupiedPlaces(sectorCopy.getMaxPlaces() - i);
        }

        List<Sector> listOfSectorsNo2 = parkingFacade.findSectorInParkingWithAvailablePlaces(parkingNo1.getId(), 0, 15, true);

        assertEquals(2, listOfSectorsNo2.size());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRED)
    @WithMockUser(roles = {Authorities.ADD_PARKING, Authorities.RESERVE_PARKING_PLACE})
    public void parkingFacadeFindParkingWithAvailablePlacesTest() throws ApplicationBaseException {
        Address addressNo1 = new Address("BoatCity", "90-000", "Pomorska");
        Address addressNo2 = new Address("BoatCity", "90-000", "Pomorska");
        Parking parkingNo1 = new Parking(addressNo1, Parking.SectorDeterminationStrategy.LEAST_OCCUPIED);
        Parking parkingNo2 = new Parking(addressNo2, Parking.SectorDeterminationStrategy.LEAST_OCCUPIED);

        parkingFacade.create(parkingNo1);
        parkingNo1.addSector("SA-01", Sector.SectorType.COVERED, 100, 200);
        parkingNo1.addSector("SA-02", Sector.SectorType.UNCOVERED, 10, 200);
        parkingNo1.addSector("SA-03", Sector.SectorType.UNCOVERED, 10, 200);
        parkingNo2.addSector("SA-04", Sector.SectorType.UNCOVERED, 10, 200);

        List<Sector> listOfSectors = parkingNo1.getSectors();
        for (int i = 0; i < listOfSectors.size(); i++) {
            listOfSectors.get(i).setOccupiedPlaces(i);
        }
        List<Parking> listOfParkingLots = parkingFacade.findParkingWithAvailablePlaces(0, 5, true);
        assertEquals(3, listOfParkingLots.size());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRED)
    @WithMockUser(roles = {Authorities.ADD_PARKING, Authorities.EDIT_SECTOR})
    public void parkingFacadeEditSectorTest() throws ApplicationBaseException {
        Address addressNo1 = new Address("BoatCity", "90-000", "Pomorska");
        Parking parkingNo1 = new Parking(addressNo1, Parking.SectorDeterminationStrategy.LEAST_OCCUPIED);

        parkingFacade.create(parkingNo1);
        parkingNo1.addSector("SA-01", Sector.SectorType.COVERED, 100, 100);
        Sector sectorNo1 = new Sector(parkingNo1, "SA-02", Sector.SectorType.COVERED, 100, 100);

        sectorNo1.setMaxPlaces(200);

        parkingFacade.editSector(sectorNo1);

        assertEquals(1, parkingNo1.getSectors().size());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRED)
    @WithMockUser(roles = {Authorities.DELETE_SECTOR})
    public void parkingFacadeRemoveSectorTest() throws Exception {
        parkingFacade.removeSector(sector);
        List<Sector> listOfSectors = new ArrayList<>();
        assertEquals(listOfSectors, parking.getSectors());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRED)
    @WithMockUser(roles = {Authorities.ENTER_PARKING_WITHOUT_RESERVATION})
    public void countSectorsReservationsNowTestAnonymousOrBasic() throws ApplicationBaseException {
        List<Sector> availableSectors = parkingFacade.getAvailableSectorsNow(Client.ClientType.BASIC, UUID.fromString("3591ced3-996e-49b4-8c56-40fe91193b1d"), LocalDateTime.now(), 24);
        //names of the sectors from script
        Sector uc1 = parkingFacade.findAndRefreshSectorById(UUID.fromString("14d51050-ffe2-4da2-abd2-4e6d06759ea5")).orElse(null);
        Sector uc2 = parkingFacade.findAndRefreshSectorById(UUID.fromString("933bcce5-a38c-4b09-bd60-2b746d9f40e8")).orElse(null);
        Sector uc3 = parkingFacade.findAndRefreshSectorById(UUID.fromString("828228e6-2fa7-418e-8cfe-7f4d79737557")).orElse(null);
        Sector uc4 = parkingFacade.findAndRefreshSectorById(UUID.fromString("38c70882-c413-467d-bdd8-c5ed5f9128d0")).orElse(null);

        Sector co1 = parkingFacade.findAndRefreshSectorById(UUID.fromString("f274420a-1322-4530-bfcb-4e515dd5a920")).orElse(null);
        Sector co2 = parkingFacade.findAndRefreshSectorById(UUID.fromString("ae65eca6-669d-43ec-8c35-39f4eb6b72bb")).orElse(null);

        Sector un1 = parkingFacade.findAndRefreshSectorById(UUID.fromString("65c51075-0749-4304-984a-9cb926e65aab")).orElse(null);
        Sector un2 = parkingFacade.findAndRefreshSectorById(UUID.fromString("b9f5bb0c-d19f-4101-ac57-d758e063ac3e")).orElse(null);

        assertTrue(availableSectors.contains(uc3));
        assertTrue(availableSectors.contains(uc4));
        assertTrue(availableSectors.contains(uc2));
        assertFalse(availableSectors.contains(uc1));

        assertFalse(availableSectors.contains(co2));
        assertFalse(availableSectors.contains(co1));

        assertFalse(availableSectors.contains(un2));
        assertFalse(availableSectors.contains(un1));

        assertEquals(3,availableSectors.size());

//        Reservation reservation1 = new Reservation(null, uc2, LocalDateTime.now());
//        reservation1.setStatus(Reservation.ReservationStatus.IN_PROGRESS);
//        reservationFacade.create(reservation1);
//
//        assertTrue(availableSectors.contains(uc3));
//        assertTrue(availableSectors.contains(uc4));
//        assertFalse(availableSectors.contains(uc2));
//        assertFalse(availableSectors.contains(uc1));
//
//        assertFalse(availableSectors.contains(co2));
//        assertFalse(availableSectors.contains(co1));
//
//        assertFalse(availableSectors.contains(un2));
//        assertFalse(availableSectors.contains(un1));
//
//        assertEquals(2,availableSectors.size());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRED)
    @WithMockUser(roles = {Authorities.ENTER_PARKING_WITHOUT_RESERVATION})
    public void countSectorsReservationsNowTestStandard() throws ApplicationBaseException {
        List<Sector> availableSectors = parkingFacade.getAvailableSectorsNow(Client.ClientType.STANDARD, UUID.fromString("3591ced3-996e-49b4-8c56-40fe91193b1d"), LocalDateTime.now(),  24);
        //names of the sectors from script
        Sector uc1 = parkingFacade.findAndRefreshSectorById(UUID.fromString("14d51050-ffe2-4da2-abd2-4e6d06759ea5")).orElse(null);
        Sector uc2 = parkingFacade.findAndRefreshSectorById(UUID.fromString("933bcce5-a38c-4b09-bd60-2b746d9f40e8")).orElse(null);
        Sector uc3 = parkingFacade.findAndRefreshSectorById(UUID.fromString("828228e6-2fa7-418e-8cfe-7f4d79737557")).orElse(null);
        Sector uc4 = parkingFacade.findAndRefreshSectorById(UUID.fromString("38c70882-c413-467d-bdd8-c5ed5f9128d0")).orElse(null);

        Sector co1 = parkingFacade.findAndRefreshSectorById(UUID.fromString("f274420a-1322-4530-bfcb-4e515dd5a920")).orElse(null);
        Sector co2 = parkingFacade.findAndRefreshSectorById(UUID.fromString("ae65eca6-669d-43ec-8c35-39f4eb6b72bb")).orElse(null);

        Sector un1 = parkingFacade.findAndRefreshSectorById(UUID.fromString("65c51075-0749-4304-984a-9cb926e65aab")).orElse(null);
        Sector un2 = parkingFacade.findAndRefreshSectorById(UUID.fromString("b9f5bb0c-d19f-4101-ac57-d758e063ac3e")).orElse(null);

        assertTrue(availableSectors.contains(uc3));
        assertTrue(availableSectors.contains(uc4));
        assertTrue(availableSectors.contains(uc2));
        assertFalse(availableSectors.contains(uc1));

        assertTrue(availableSectors.contains(co2));
        assertFalse(availableSectors.contains(co1));

        assertFalse(availableSectors.contains(un2));
        assertFalse(availableSectors.contains(un1));

        assertEquals(4,availableSectors.size());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRED)
    @WithMockUser(roles = {Authorities.ENTER_PARKING_WITHOUT_RESERVATION})
    public void countSectorsReservationsNowTestPremium() throws ApplicationBaseException {
        List<Sector> availableSectors = parkingFacade.getAvailableSectorsNow(Client.ClientType.PREMIUM, UUID.fromString("3591ced3-996e-49b4-8c56-40fe91193b1d"), LocalDateTime.now(), 24);
        //names of the sectors from script
        Sector uc1 = parkingFacade.findAndRefreshSectorById(UUID.fromString("14d51050-ffe2-4da2-abd2-4e6d06759ea5")).orElse(null);
        Sector uc2 = parkingFacade.findAndRefreshSectorById(UUID.fromString("933bcce5-a38c-4b09-bd60-2b746d9f40e8")).orElse(null);
        Sector uc3 = parkingFacade.findAndRefreshSectorById(UUID.fromString("828228e6-2fa7-418e-8cfe-7f4d79737557")).orElse(null);
        Sector uc4 = parkingFacade.findAndRefreshSectorById(UUID.fromString("38c70882-c413-467d-bdd8-c5ed5f9128d0")).orElse(null);

        Sector co1 = parkingFacade.findAndRefreshSectorById(UUID.fromString("f274420a-1322-4530-bfcb-4e515dd5a920")).orElse(null);
        Sector co2 = parkingFacade.findAndRefreshSectorById(UUID.fromString("ae65eca6-669d-43ec-8c35-39f4eb6b72bb")).orElse(null);

        Sector un1 = parkingFacade.findAndRefreshSectorById(UUID.fromString("65c51075-0749-4304-984a-9cb926e65aab")).orElse(null);
        Sector un2 = parkingFacade.findAndRefreshSectorById(UUID.fromString("b9f5bb0c-d19f-4101-ac57-d758e063ac3e")).orElse(null);

        assertTrue(availableSectors.contains(uc3));
        assertTrue(availableSectors.contains(uc4));
        assertTrue(availableSectors.contains(uc2));
        assertFalse(availableSectors.contains(uc1));

        assertTrue(availableSectors.contains(co2));
        assertFalse(availableSectors.contains(co1));

        assertTrue(availableSectors.contains(un2));
        assertFalse(availableSectors.contains(un1));

        assertEquals(5,availableSectors.size());
    }

//    @Test
//    @Transactional(propagation = Propagation.REQUIRED)
//    public void parkingFacadeFindSectorInParkingBySectorTypesTest() throws Exception {
//        Address addressNo1 = new Address("tt", "Mi", "ttt");
//        Address addressNo2 = new Address("dtt", "Mei", "ttet");
//        Parking parkingNo1 = new Parking(addressNo1);
//        Parking parkingNo2 = new Parking(addressNo2);
//
//        parkingNo1.addSector("Se2", Sector.SectorType.UNCOVERED, 20, 200);
//        parkingNo1.addSector("St3", Sector.SectorType.UNCOVERED, 30, 300);
//        parkingNo1.addSector("St4", Sector.SectorType.UNCOVERED, 30, 300);
//        parkingNo2.addSector("St42", Sector.SectorType.UNCOVERED, 30, 300);
//
//        List<Sector.SectorType> sectorTypes = List.of(Sector.SectorType.COVERED);
//        List<Sector> listOfSectorsNo1 = parkingFacade.findSectorInParkingBySectorTypes(parkingNo1.getId(), sectorTypes, 0, 10, true);
//        List<Sector> listOfSectorsNo2 = parkingFacade.findSectorInParkingBySectorTypes(parkingNo1.getId(), sectorTypes, 0, 10, true);
//
//        assertEquals(0, listOfSectorsNo1.size());
//        assertEquals(0, listOfSectorsNo2.size());
//    }
}
