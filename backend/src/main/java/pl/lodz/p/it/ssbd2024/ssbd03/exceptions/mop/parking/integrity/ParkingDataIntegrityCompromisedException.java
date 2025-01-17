package pl.lodz.p.it.ssbd2024.ssbd03.exceptions.mop.parking.integrity;

import pl.lodz.p.it.ssbd2024.ssbd03.exceptions.mop.parking.ParkingBaseException;
import pl.lodz.p.it.ssbd2024.ssbd03.utils.I18n;
import pl.lodz.p.it.ssbd2024.ssbd03.commons.dto.mop.parkingDTO.ParkingSignableDTO;

/**
 * Used to specify an Exception related with trying to modify signature-protected Parking properties.
 * @see ParkingSignableDTO
 */
public class ParkingDataIntegrityCompromisedException extends ParkingBaseException {

    public ParkingDataIntegrityCompromisedException(){
        super(I18n.DATA_INTEGRITY_COMPROMISED);
    }

    public ParkingDataIntegrityCompromisedException(String message) {
        super(message);
    }
}
