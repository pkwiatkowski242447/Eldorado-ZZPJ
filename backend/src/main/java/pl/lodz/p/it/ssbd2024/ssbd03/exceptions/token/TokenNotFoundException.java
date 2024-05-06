package pl.lodz.p.it.ssbd2024.ssbd03.exceptions.token;

/**
 * Used to specify an Exception related trying to access a Token that doesn't exist.
 * @see pl.lodz.p.it.ssbd2024.ssbd03.entities.Token
 */
public class TokenNotFoundException extends Exception{
    public TokenNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public TokenNotFoundException(String message) {
        super(message);
    }
}