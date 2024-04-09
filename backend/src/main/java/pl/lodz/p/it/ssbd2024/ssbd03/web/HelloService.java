package pl.lodz.p.it.ssbd2024.ssbd03.web;

import jakarta.transaction.Transactional;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.lodz.p.it.ssbd2024.ssbd03.entities.HelloEntity;
import pl.lodz.p.it.ssbd2024.ssbd03.entities.HelloEntity2;
import pl.lodz.p.it.ssbd2024.ssbd03.entities.mop.Address;
import pl.lodz.p.it.ssbd2024.ssbd03.entities.mop.Parking;
import pl.lodz.p.it.ssbd2024.ssbd03.entities.mop.Sector;
import pl.lodz.p.it.ssbd2024.ssbd03.mop.facades.ParkingFacade;
import pl.lodz.p.it.ssbd2024.ssbd03.repostories.HelloRepoMOK;
import pl.lodz.p.it.ssbd2024.ssbd03.repostories.HelloRepoMOP;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@NoArgsConstructor
public class HelloService {

    private HelloRepoMOK repoMOK;
    private HelloRepoMOP repoMOP;

    private ParkingFacade parkingFacade;

    private Parking parkingSer;

    @Autowired
    public HelloService(HelloRepoMOK repoMOK, HelloRepoMOP repoMOP, ParkingFacade parkingFacade) {
        this.repoMOK = repoMOK;
        this.repoMOP = repoMOP;
        this.parkingFacade = parkingFacade;

        Parking parking = getTestParking("a");
        parkingFacade.create(parking);
//        Parking parking1 = getTestParking("b");
//        parkingFacade.create(parking1);
        parkingSer = parking;

        System.out.println("asd ID 1 parkingu: "+parkingFacade.find(parking.getId()).orElse(null).getId());
        System.out.println("asd liczba parkingow 1: "+parkingFacade.findAll().size());
        System.out.println("asd liczba sektorow w parkingu: "+parkingFacade.findAndRefresh(parking.getId()).orElse(null).getSectors().size());
    }

    @Transactional
    public String getHello() {

        Parking parking = parkingFacade.find(parkingSer.getId()).orElse(null);
        System.out.println("asd liczba sektorow: " + parking.getSectors().size());

        parkingFacade.removeSector(parking.getSectors().get(0));
        System.out.println("usunieto");
        //parking = parkingFacade.findAndRefresh(parking.getId()).orElse(null);
        System.out.println("asd liczba sektorow w bazie po parkingu: " + parking.getSectors().size());
//
//        parkingFacade.remove(parking1);
//        System.out.println("asd liczba parkingow po probie usuniecia: "+parkingFacade.findAll().size());
        return "Hello World";
    }

    public void addTestEnt() {
        HelloEntity ent = new HelloEntity(null, "Jan", 20);
        repoMOK.save(ent);
        HelloEntity2 ent2 = new HelloEntity2(null, "Miroslaw", 18, LocalDateTime.now());
        repoMOP.save(ent2);
    }

    public Parking getTestParking(String seed) {
        Parking parking = new Parking();
        Address address = new Address("Lodz", "12-345", "ulica" + seed);
        parking.setAddress(address);
        parking.addSector("s1", Sector.SectorType.UNCOVERED, 100, 1);
        parking.addSector("s2", Sector.SectorType.COVERED, 120, 2);
        parking.addSector("s3", Sector.SectorType.UNDERGROUND, 20, 3);

        return parking;
    }
}

