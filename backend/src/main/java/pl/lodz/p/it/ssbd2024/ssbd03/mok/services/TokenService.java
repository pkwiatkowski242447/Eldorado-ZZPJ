package pl.lodz.p.it.ssbd2024.ssbd03.mok.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.lodz.p.it.ssbd2024.ssbd03.entities.Token;
import pl.lodz.p.it.ssbd2024.ssbd03.entities.mok.Account;
import pl.lodz.p.it.ssbd2024.ssbd03.mok.facades.TokenFacade;
import pl.lodz.p.it.ssbd2024.ssbd03.utils.providers.JWTProvider;

import java.util.UUID;

/**
 * Service managing Tokens.
 *
 * @see Token
 */
@Slf4j
@Service
public class TokenService {

    private final TokenFacade tokenFacade;
    private final JWTProvider jwtProvider;

    /**
     * Autowired constructor for the service.
     *
     * @param tokenFacade   Facade containing method for token manipulation.
     * @param jwtProvider   Component used to provide JWT token generation mechanism for a certain payload.
     */
    @Autowired
    public TokenService(TokenFacade tokenFacade,
                        JWTProvider jwtProvider) {
        this.tokenFacade = tokenFacade;
        this.jwtProvider = jwtProvider;
    }

    /**
     * Creates and persists registration token for the Account.
     *
     * @param account Account for which the token is created.
     * @return Returns newly created registration token value.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public String createRegistrationToken(Account account) {
        String tokenValue = this.jwtProvider.generateActionToken(account,24);

        Token registrationToken = new Token(tokenValue, account, Token.TokenType.REGISTER);
        this.tokenFacade.create(registrationToken);

        return tokenValue;
    }

    /**
     * Creates and persists email confirmation token for the Account.
     *
     * @param account Account for which the token is created.
     * @return Returns newly created email confirmation token value.
     */
    public String createEmailConfirmationToken(Account account, String newEmail) {

        Token emailConfirmationToken = new Token(newEmail, account, Token.TokenType.CONFIRM_EMAIL);
        this.tokenFacade.create(emailConfirmationToken);

        return newEmail;
    }

    /**
     * Creates and persists E-mail confirmation Token for the Account.
     *
     * @param account Account for which the token is created.
     * @return Json Web Token from the Token.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public String createEmailConfirmationToken(Account account) {
        String tokenValue = this.jwtProvider.generateActionToken(account,24);

        Token emailToken = new Token(tokenValue, account, Token.TokenType.CONFIRM_EMAIL);
        this.tokenFacade.create(emailToken);

        return emailToken.getTokenValue();
    }
}
