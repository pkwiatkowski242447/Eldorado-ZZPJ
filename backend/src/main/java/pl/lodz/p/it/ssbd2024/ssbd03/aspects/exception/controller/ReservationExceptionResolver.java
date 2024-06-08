package pl.lodz.p.it.ssbd2024.ssbd03.aspects.exception.controller;

import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import pl.lodz.p.it.ssbd2024.ssbd03.commons.dto.mok.exception.ExceptionDTO;
import pl.lodz.p.it.ssbd2024.ssbd03.exceptions.mop.reservation.ReservationNoAvailablePlaceException;
import pl.lodz.p.it.ssbd2024.ssbd03.exceptions.mop.reservation.read.ReservationNotFoundException;
import pl.lodz.p.it.ssbd2024.ssbd03.exceptions.mop.reservation.status.ReservationStatusException;

/**
 * General exception handling component in a form of @ControllerAdvice for Reservation exceptions.
 */
@ControllerAdvice
@Order(1)
public class ReservationExceptionResolver {

    /**
     * This method transforms any ReservationStatusException, propagating from the controller layer
     * to the HTTP response containing internationalization key with status code, which in this case is
     * 400 BAD REQUEST.
     *
     * @param exception Exception of type ReservationStatusException (or subclass of that exception), propagating from
     *                  the controller layer.
     * @return HTTP Response with status 400 BAD REQUEST and internationalization key, which is located in the
     * Response body.
     */
    @ExceptionHandler(value = {ReservationStatusException.class})
    public ResponseEntity<?> handleReservationStatusException(ReservationStatusException exception) {
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ExceptionDTO(exception.getMessage()));
    }

    /**
     * This method transforms any ReservationNoAvailablePlaceException, propagating from the controller layer
     * to the HTTP response containing internationalization key with status code, which in this case is
     * 400 BAD REQUEST.
     *
     * @param exception Exception of type ReservationNoAvailablePlaceException (or subclass of that exception),
     *                  propagating from the controller layer.
     * @return HTTP Response with status 400 BAD REQUEST and internationalization key, which is located in the
     * Response body.
     */
    @ExceptionHandler(value = {ReservationNoAvailablePlaceException.class})
    public ResponseEntity<?> handleReservationNoAvailablePlaceException(ReservationNoAvailablePlaceException exception) {
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ExceptionDTO(exception.getMessage()));
    }

    /**
     * This method transforms any ReservationNotFoundException, propagating from the controller layer
     * to the HTTP response containing internationalization key with status code, which in this case is
     * 400 BAD REQUEST.
     *
     * @param exception Exception of type ReservationNotFoundException (or subclass of that exception),
     *                  propagating from the controller layer.
     * @return HTTP Response with status 400 BAD REQUEST and internationalization key, which is located in the
     * Response body.
     */
    @ExceptionHandler(value = {ReservationNotFoundException.class})
    public ResponseEntity<?> handleReservationNotFoundException(ReservationNotFoundException exception) {
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ExceptionDTO(exception.getMessage()));
    }
}