package pl.lodz.p.it.ssbd2024.ssbd03.mok.services.implementations;

import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.lodz.p.it.ssbd2024.ssbd03.aspects.logging.TxTracked;
import pl.lodz.p.it.ssbd2024.ssbd03.config.security.consts.Roles;
import pl.lodz.p.it.ssbd2024.ssbd03.entities.Token;
import pl.lodz.p.it.ssbd2024.ssbd03.entities.mok.*;
import pl.lodz.p.it.ssbd2024.ssbd03.exceptions.ApplicationBaseException;
import pl.lodz.p.it.ssbd2024.ssbd03.exceptions.ApplicationOptimisticLockException;
import pl.lodz.p.it.ssbd2024.ssbd03.exceptions.account.read.AccountNotFoundException;
import pl.lodz.p.it.ssbd2024.ssbd03.exceptions.account.AccountUserLevelException;
import pl.lodz.p.it.ssbd2024.ssbd03.exceptions.account.conflict.AccountAlreadyBlockedException;
import pl.lodz.p.it.ssbd2024.ssbd03.exceptions.account.conflict.AccountAlreadyUnblockedException;
import pl.lodz.p.it.ssbd2024.ssbd03.exceptions.account.integrity.UserLevelMissingException;
import pl.lodz.p.it.ssbd2024.ssbd03.exceptions.account.conflict.AccountEmailAlreadyTakenException;
import pl.lodz.p.it.ssbd2024.ssbd03.exceptions.account.conflict.AccountSameEmailException;
import pl.lodz.p.it.ssbd2024.ssbd03.exceptions.account.read.AccountEmailNullException;
import pl.lodz.p.it.ssbd2024.ssbd03.exceptions.account.read.AccountEmailNotFoundException;
import pl.lodz.p.it.ssbd2024.ssbd03.exceptions.account.read.AccountIdNotFoundException;
import pl.lodz.p.it.ssbd2024.ssbd03.exceptions.account.resetOwnPassword.CurrentPasswordAndNewPasswordAreTheSameException;
import pl.lodz.p.it.ssbd2024.ssbd03.exceptions.account.resetOwnPassword.IncorrectPasswordException;
import pl.lodz.p.it.ssbd2024.ssbd03.exceptions.account.status.AccountBlockedException;
import pl.lodz.p.it.ssbd2024.ssbd03.exceptions.account.status.AccountNotActivatedException;
import pl.lodz.p.it.ssbd2024.ssbd03.exceptions.account.validation.AccountConstraintViolationException;
import pl.lodz.p.it.ssbd2024.ssbd03.exceptions.token.read.TokenNotFoundException;
import pl.lodz.p.it.ssbd2024.ssbd03.exceptions.token.TokenNotValidException;
import pl.lodz.p.it.ssbd2024.ssbd03.mok.facades.AccountMOKFacade;
import pl.lodz.p.it.ssbd2024.ssbd03.mok.facades.TokenFacade;
import pl.lodz.p.it.ssbd2024.ssbd03.mok.facades.UserLevelFacade;
import pl.lodz.p.it.ssbd2024.ssbd03.mok.services.interfaces.AccountServiceInterface;
import pl.lodz.p.it.ssbd2024.ssbd03.utils.I18n;
import pl.lodz.p.it.ssbd2024.ssbd03.utils.providers.JWTProvider;
import pl.lodz.p.it.ssbd2024.ssbd03.utils.providers.MailProvider;

import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Service managing Accounts.
 *
 * @see Account
 */
@Slf4j
@Service
@TxTracked
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class AccountService implements AccountServiceInterface {

    @Value("${mail.account.creation.confirmation.url}")
    private String accountCreationConfirmationUrl;

    @Value("${mail.account.reset.password.url}")
    private String accountPasswordResetUrl;

    @Value("${mail.account.confirm.email.url}")
    private String accountConfirmEmail;

    @Value("${account.creation.confirmation.period.length.hours}")
    private int accountCreationConfirmationPeriodLengthHours;
    @Value("${account.password.reset.period.length.minutes}")
    private int passwordResetPeriodLengthMinutes;
    @Value("${email.change.confirmation.period.length.hours}")
    private int emailChangeConfirmationPeriodLengthHours;

    /**
     * AccountFacade used for operations on account entities.
     */
    private final AccountMOKFacade accountFacade;
    /**
     * PasswordEncoder used for encoding passwords.
     */
    private final PasswordEncoder passwordEncoder;
    /**
     * TokenFacade used for operations on token entities.
     */
    private final TokenFacade tokenFacade;
    /**
     * Component used to send e-mail messages to user e-mail address (depending on the actions they perform).
     */
    private final MailProvider mailProvider;
    /**
     * TokenProvider used for operations on TOKENS.
     */
    private final JWTProvider jwtProvider;
    /**
     * TokenServiceInterface used for operations on tokens.
     */

    private final UserLevelFacade userLevelFacade;

    /**
     * Autowired constructor for the service.
     *
     * @param accountFacade   Facade responsible for users accounts management.
     * @param passwordEncoder This component is used to generate hashed passwords for user accounts (not to store them in their original form).
     * @param tokenFacade     This facade is responsible for manipulating tokens, used for various, user account related operations.
     * @param mailProvider    This component is used to send e-mail messages to e-mail address of users (where message depends on their actions).
     * @param jwtProvider     This component is used to generate token values for token facade.
     * @param userLevelFacade It is used to create new tokens, remove them, etc.
     */
    @Autowired
    public AccountService(AccountMOKFacade accountFacade,
                          PasswordEncoder passwordEncoder,
                          TokenFacade tokenFacade,
                          MailProvider mailProvider,
                          JWTProvider jwtProvider,
                          UserLevelFacade userLevelFacade) {
        this.accountFacade = accountFacade;
        this.passwordEncoder = passwordEncoder;
        this.tokenFacade = tokenFacade;
        this.mailProvider = mailProvider;
        this.jwtProvider = jwtProvider;
        this.userLevelFacade = userLevelFacade;
    }

    /**
     * Create new account, which will have default user level of Client.
     *
     * @param login       User login, used in order to authenticate to the application.
     * @param password    User password, used in combination with login to authenticate to the application.
     * @param firstName   First name of the user.
     * @param lastName    Last name of the user.
     * @param email       Email address, which will be used to send messages (e.g. confirmation messages) for actions in the application.
     * @param phoneNumber Phone number of the user.
     * @param language    Predefined language constant used for internationalizing all messages for user (initially browser value constant but could be set).
     * @throws ApplicationBaseException Superclass for all exceptions that could be thrown by the aspect, intercepting facade create method.
     */
    @Override
    @RolesAllowed({Roles.ANONYMOUS, Roles.ADMIN})
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = ApplicationBaseException.class)
    public void registerClient(String login, String password, String firstName, String lastName, String email, String phoneNumber, String language)
            throws ApplicationBaseException {
        Account newClientAccount = new Account(login, passwordEncoder.encode(password), firstName, lastName, email, phoneNumber);
        newClientAccount.setAccountLanguage(language);
        UserLevel clientLevel = new Client();
        clientLevel.setAccount(newClientAccount);
        newClientAccount.addUserLevel(clientLevel);

        this.accountFacade.create(newClientAccount);

        String tokenValue = jwtProvider.generateActionToken(newClientAccount, this.accountCreationConfirmationPeriodLengthHours, ChronoUnit.HOURS);
        tokenFacade.create(new Token(tokenValue, newClientAccount, Token.TokenType.REGISTER));

        String encodedTokenValue = new String(Base64.getUrlEncoder().encode(tokenValue.getBytes()));
        String confirmationURL = this.accountCreationConfirmationUrl + encodedTokenValue;

        mailProvider.sendRegistrationConfirmEmail(newClientAccount.getName(),
                newClientAccount.getLastname(),
                newClientAccount.getEmail(),
                confirmationURL,
                newClientAccount.getAccountLanguage());
    }

    /**
     * This method is used to create new account, which will have default user level of Staff, create
     * appropriate register token, save it to the database, and at the - send the account activation
     * email to the given email address.
     *
     * @param login       User login, used in order to authenticate to the application.
     * @param password    User password, used in combination with login to authenticate to the application.
     * @param firstName   First name of the user.
     * @param lastName    Last name of the user.
     * @param email       Email address, which will be used to send messages (e.g. confirmation messages) for actions in the application.
     * @param phoneNumber Phone number of the user.
     * @param language    Predefined language constant used for internationalizing all messages for user (initially browser constant value but could be set).
     * @throws ApplicationBaseException This exception will be thrown if any Persistence exception occurs.
     */
    @Override
    @RolesAllowed({Roles.ADMIN})
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = ApplicationBaseException.class)
    public void registerStaff(String login, String password, String firstName, String lastName, String email, String phoneNumber, String language) throws ApplicationBaseException {
        Account newStaffAccount = new Account(login, passwordEncoder.encode(password), firstName, lastName, email, phoneNumber);
        newStaffAccount.setAccountLanguage(language);
        UserLevel staffUserLevel = new Staff();
        staffUserLevel.setAccount(newStaffAccount);
        newStaffAccount.addUserLevel(staffUserLevel);

        accountFacade.create(newStaffAccount);

        String tokenValue = jwtProvider.generateActionToken(newStaffAccount, this.accountCreationConfirmationPeriodLengthHours, ChronoUnit.HOURS);
        tokenFacade.create(new Token(tokenValue, newStaffAccount, Token.TokenType.REGISTER));

        String encodedTokenValue = new String(Base64.getUrlEncoder().encode(tokenValue.getBytes()));
        String confirmationURL = this.accountCreationConfirmationUrl + encodedTokenValue;

        mailProvider.sendRegistrationConfirmEmail(newStaffAccount.getName(),
                newStaffAccount.getLastname(),
                newStaffAccount.getEmail(),
                confirmationURL,
                newStaffAccount.getAccountLanguage());
    }

    /**
     * This method is used to create new account, which will have default user level of Admin, create
     * appropriate register token, save it to the database, and at the - send the account activation
     * email to the given email address.
     *
     * @param login       User login, used in order to authenticate to the application.
     * @param password    User password, used in combination with login to authenticate to the application.
     * @param firstName   First name of the user.
     * @param lastName    Last name of the user.
     * @param email       Email address, which will be used to send messages (e.g. confirmation messages) for actions in the application.
     * @param phoneNumber Phone number of the user.
     * @param language    Predefined language constant used for internationalizing all messages for user (initially browser constant value but could be set).
     * @throws ApplicationBaseException This exception will be thrown if any Persistence exception occurs.
     */
    @Override
    @RolesAllowed({Roles.ADMIN})
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = ApplicationBaseException.class)
    public void registerAdmin(String login, String password, String firstName, String lastName, String email, String phoneNumber, String language) throws ApplicationBaseException {
        Account newAdminAccount = new Account(login, passwordEncoder.encode(password), firstName, lastName, email, phoneNumber);
        newAdminAccount.setAccountLanguage(language);
        UserLevel adminUserLevel = new Admin();
        adminUserLevel.setAccount(newAdminAccount);
        newAdminAccount.addUserLevel(adminUserLevel);

        accountFacade.create(newAdminAccount);

        String tokenValue = jwtProvider.generateActionToken(newAdminAccount, this.accountCreationConfirmationPeriodLengthHours, ChronoUnit.HOURS);
        tokenFacade.create(new Token(tokenValue, newAdminAccount, Token.TokenType.REGISTER));

        String encodedTokenValue = new String(Base64.getUrlEncoder().encode(tokenValue.getBytes()));
        String confirmationURL = this.accountCreationConfirmationUrl + encodedTokenValue;

        mailProvider.sendRegistrationConfirmEmail(newAdminAccount.getName(),
                newAdminAccount.getLastname(),
                newAdminAccount.getEmail(),
                confirmationURL,
                newAdminAccount.getAccountLanguage());
    }

    /**
     * This method is used to initiate process of resetting current user account password. This method basically
     * generates a token of type CHANGE_PASSWORD and writes it to the database, and then sends a password change URL to the e-mail address
     * specified by the user in the form and send to the application with the usage of DTO object.
     *
     * @param userEmail Email address that will be used to search for the existing account, and then used for sending
     *                  e-mail message with password change URL.
     */
    @RolesAllowed({Roles.ANONYMOUS})
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = ApplicationBaseException.class)
    public void forgetAccountPassword(String userEmail) throws ApplicationBaseException {
        Account account = this.accountFacade.findByEmail(userEmail).orElseThrow(AccountEmailNotFoundException::new);

        if (account.getBlocked()) throw new AccountBlockedException();
        else if (!account.getActive()) throw new AccountNotActivatedException();

        tokenFacade.findByTypeAndAccount(Token.TokenType.RESET_PASSWORD, account.getId()).ifPresent(tokenFacade::remove);

        String tokenValue = this.jwtProvider.generateActionToken(account, this.passwordResetPeriodLengthMinutes, ChronoUnit.MINUTES);
        Token passwordToken = new Token(tokenValue, account, Token.TokenType.RESET_PASSWORD);
        this.tokenFacade.create(passwordToken);

        String encodedTokenValue = new String(Base64.getUrlEncoder().encode(tokenValue.getBytes()));
        String passwordResetURL = this.accountPasswordResetUrl + encodedTokenValue;

        this.mailProvider.sendPasswordResetEmail(account.getName(),
                account.getLastname(),
                userEmail,
                passwordResetURL,
                account.getAccountLanguage());
    }

    /**
     * This method is used to change password of the user. This method does read RESET PASSWORD token with
     * specified token value, and then
     *
     * @param token       Value of the token, that will be used to find RESET PASSWORD token in the database.
     * @param newPassword New password, transferred to the web application by data transfer object.
     * @throws ApplicationBaseException General superclass for all exceptions thrown by the aspects intercepting that
     *                                  method.
     */
    @RolesAllowed({Roles.ANONYMOUS})
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = ApplicationBaseException.class)
    public void changeAccountPassword(String token, String newPassword) throws ApplicationBaseException {
        String decodedTokenValue = new String(Base64.getUrlDecoder().decode(token.getBytes()));
        Token tokenObject = this.tokenFacade.findByTokenValue(decodedTokenValue)
                .orElseThrow(() -> new TokenNotFoundException(I18n.TOKEN_VALUE_NOT_FOUND_EXCEPTION));
        if (!jwtProvider.isTokenValid(tokenObject.getTokenValue(), tokenObject.getAccount()))
            throw new TokenNotValidException(I18n.TOKEN_NOT_VALID_EXCEPTION);

        Account account = this.accountFacade.findAndRefresh(tokenObject.getAccount().getId()).orElseThrow(AccountIdNotFoundException::new);
        if (account.getBlocked()) throw new AccountBlockedException();
        else if (!account.getActive()) throw new AccountNotActivatedException();

        account.setPassword(this.passwordEncoder.encode(newPassword));
        this.accountFacade.edit(account);

        this.tokenFacade.remove(tokenObject);
    }

    /**
     * Method for blocking an account by its UUID.
     *
     * @param id Account identifier
     * @throws AccountNotFoundException       Threw when there is no account with given login.
     * @throws AccountAlreadyBlockedException Threw when the account is already blocked.
     */
    @Override
    @RolesAllowed({Roles.ADMIN})
    public void blockAccount(UUID id) throws ApplicationBaseException, AccountNotFoundException, AccountAlreadyBlockedException {
        Account account = accountFacade.findAndRefresh(id).orElseThrow(AccountNotFoundException::new);
        if (account.getBlocked() && account.getBlockedTime() == null) {
            throw new AccountAlreadyBlockedException();
        }

        account.blockAccount(true);
        accountFacade.edit(account);

        // Sending information email
        mailProvider.sendBlockAccountInfoEmail(account.getName(), account.getLastname(),
                account.getEmail(), account.getAccountLanguage(), true);
    }

    /**
     * Method for unblocking an account by its UUID.
     *
     * @param id Account identifier
     * @throws AccountNotFoundException         Threw when there is no account with given login.
     * @throws AccountAlreadyUnblockedException Threw when the account is already unblocked.
     */
    @Override
    @RolesAllowed({Roles.ADMIN})
    public void unblockAccount(UUID id) throws ApplicationBaseException {
        Account account = accountFacade.findAndRefresh(id).orElseThrow(AccountNotFoundException::new);
        if (!account.getBlocked()) {
            throw new AccountAlreadyUnblockedException();
        }

        account.unblockAccount();
        accountFacade.edit(account);

        // Sending information email
        mailProvider.sendUnblockAccountInfoEmail(account.getName(), account.getLastname(),
                account.getEmail(), account.getAccountLanguage());
    }

    /**
     * This method is used to modify user personal data.
     *
     * @param modifiedAccount Account with potentially modified properties: name, lastname, phoneNumber.
     * @param userLogin       Login associated with the modified account.
     * @return Account object with applied modifications
     * @throws AccountNotFoundException           Threw if the account with passed login property does not exist.
     * @throws ApplicationOptimisticLockException Threw while editing the account, a parallel editing action occurred.
     */
    @Override
    @RolesAllowed({ Roles.AUTHENTICATED })
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = AccountConstraintViolationException.class)
    public Account modifyAccount(Account modifiedAccount, String userLogin) throws ApplicationBaseException {
        Account foundAccount = accountFacade.findByLogin(userLogin).orElseThrow(AccountNotFoundException::new);

        if (!modifiedAccount.getVersion().equals(foundAccount.getVersion())) {
            throw new ApplicationOptimisticLockException();
        }

        foundAccount.setName(modifiedAccount.getName());
        foundAccount.setLastname(modifiedAccount.getLastname());
        foundAccount.setPhoneNumber(modifiedAccount.getPhoneNumber());
        foundAccount.setTwoFactorAuth(modifiedAccount.getTwoFactorAuth());

        for (UserLevel foundUserLevel : foundAccount.getUserLevels()) {
            UserLevel modifiedUserLevel = modifiedAccount.getUserLevels().stream()
                    .filter((level) -> level.getClass().getSimpleName().equalsIgnoreCase(foundUserLevel.getClass().getSimpleName()))
                    .findFirst().orElseThrow(UserLevelMissingException::new);

            if (!foundUserLevel.getVersion().equals(modifiedUserLevel.getVersion())) {
                throw new ApplicationOptimisticLockException();
            }
        }

        accountFacade.edit(foundAccount);

        return foundAccount;
    }

    /**
     * Activate account with a token from activation URL, sent to user e-mail address, specified during registration.
     *
     * @param token Last part of the activation URL sent in a message to users e-mail address.
     * @return Boolean value indicating whether activation of the account was successful or not.
     * @throws ApplicationBaseException General superclass for all exceptions thrown by aspects intercepting this method.
     */
    @Override
    @RolesAllowed({Roles.ANONYMOUS})
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = ApplicationBaseException.class)
    public boolean activateAccount(String token) throws ApplicationBaseException {
        String decodedTokenValue = new String(Base64.getUrlDecoder().decode(token.getBytes()));
        Token tokenFromDB = tokenFacade.findByTokenValue(decodedTokenValue).orElseThrow(() -> new TokenNotFoundException(I18n.TOKEN_VALUE_NOT_FOUND_EXCEPTION));
        Account account = accountFacade.find(jwtProvider.extractAccountId(tokenFromDB.getTokenValue()))
                .orElseThrow(AccountIdNotFoundException::new);
        if (jwtProvider.isTokenValid(tokenFromDB.getTokenValue(), account)) {
            log.info(account.toString());
            account.setActive(true);
            account.setVerified(true);
            accountFacade.edit(account);
            tokenFacade.remove(tokenFromDB);
            mailProvider.sendActivationConfirmationEmail(account.getName(),
                    account.getLastname(),
                    account.getEmail(),
                    account.getAccountLanguage());
            return true;
        }
        return false;
    }

    /**
     * Confirm e-mail change with a token from confirmation URL, sent to the new e-mail address.
     *
     * @param token Last part of the confirmation URL sent in a message to user's e-mail address.
     * @return Returns true if the e-mail confirmation was successful. Returns false if the token is expired or invalid.
     * @throws AccountNotFoundException  Threw if the account connected to the token does not exist.
     * @throws TokenNotFoundException    Threw if the token does not exist in the database.
     * @throws AccountEmailNullException Threw if the email extracted from the token was for some strange reason null.
     */
    @Override
    @RolesAllowed({Roles.ANONYMOUS})
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = ApplicationBaseException.class)
    public boolean confirmEmail(String token) throws ApplicationBaseException {
        String decodedTokenValue = new String(Base64.getUrlDecoder().decode(token.getBytes()));
        Token tokenFromDB = tokenFacade.findByTokenValue(decodedTokenValue).orElseThrow(() -> new TokenNotFoundException(I18n.TOKEN_NOT_FOUND_EXCEPTION));

        Optional<Account> accountFromDB = accountFacade.find(jwtProvider.extractAccountId(decodedTokenValue));
        Account account = accountFromDB.orElseThrow(() -> new AccountNotFoundException(I18n.ACCOUNT_NOT_FOUND_EXCEPTION));

        if (jwtProvider.isTokenValid(decodedTokenValue, account)) {
            String email = jwtProvider.extractEmail(decodedTokenValue);
            if (email == null) throw new AccountEmailNullException(I18n.ACCOUNT_EMAIL_FROM_TOKEN_NULL_EXCEPTION);
            account.setEmail(email);
            accountFacade.edit(account);
            tokenFacade.remove(tokenFromDB);
            return true;
        }
        return false;
    }

    /**
     * Retrieve Account that match the parameters, in a given order.
     *
     * @param login      Account's login. A phrase is sought in the logins.
     * @param firstName  Account's owner first name. A phrase is sought in the names.
     * @param lastName   Account's owner last name. A phrase is sought in the last names.
     * @param order      Ordering in which user accounts should be returned.
     * @param pageNumber Number of the page with searched users accounts.
     * @param pageSize   Number of the users accounts per page.
     * @return List of user accounts that match the given parameters.
     */
    @Override
    @RolesAllowed({Roles.ADMIN})
    public List<Account> getAccountsByMatchingLoginFirstNameAndLastName(String login,
                                                                        String firstName,
                                                                        String lastName,
                                                                        boolean active,
                                                                        boolean order,
                                                                        int pageNumber,
                                                                        int pageSize) {
        return accountFacade.findAllAccountsByActiveAndLoginAndUserFirstNameAndUserLastNameWithPagination(login, firstName, lastName, active, order, pageNumber, pageSize);
    }

    /**
     * Retrieve all accounts in the system.
     *
     * @param pageNumber The page number of the results to return.
     * @param pageSize   The number of results to return per page.
     * @return A list of all accounts in the system, ordered by account login, with pagination applied.
     */
    @Override
    @RolesAllowed({Roles.ADMIN})
    public List<Account> getAllAccounts(int pageNumber, int pageSize) {
        return accountFacade.findAllAccountsWithPagination(pageNumber, pageSize);
    }

    /**
     * Retrieves an Account by the login.
     *
     * @param login Login of the searched user account.
     * @return If Account with the given login was found returns Account, otherwise throws AccountNotFoundException.
     * @throws AccountNotFoundException Thrown when account from security context can't be found in the database.
     */
    @Override
    @RolesAllowed({Roles.AUTHENTICATED})
    @Transactional(propagation = Propagation.REQUIRED)
    public Account getAccountByLogin(String login) throws ApplicationBaseException {
        return accountFacade.findByLogin(login).orElseThrow(AccountNotFoundException::new);
    }

    /**
     * Retrieves from the database account by id.
     *
     * @param id Account's id.
     * @return If Account with the given id was found returns Account, otherwise throws AccountNotFoundException.
     * @throws AccountNotFoundException Thrown when account from security context can't be found in the database.
     */
    @Override
    public Account getAccountById(UUID id) throws ApplicationBaseException {
        return accountFacade.findAndRefresh(id).orElseThrow(AccountNotFoundException::new);
    }

    /**
     * Changes the e-mail of the specified Account.
     *
     * @param accountId ID of the account which the e-mail will be changed.
     * @param newEmail  New e-mail address.
     * @throws ApplicationBaseException General superclass for all exceptions thrown by exception handling aspects in facade layer.
     */
    @Override
    @RolesAllowed({Roles.AUTHENTICATED})
    @Transactional(propagation = Propagation.REQUIRED)
    public void changeEmail(UUID accountId, String newEmail) throws ApplicationBaseException {
        Account account = accountFacade.find(accountId).orElseThrow(AccountNotFoundException::new);
        if (Objects.equals(account.getEmail(), newEmail))
            throw new AccountSameEmailException();
        if (accountFacade.findByEmail(newEmail).isPresent())
            throw new AccountEmailAlreadyTakenException();

        tokenFacade.findByTypeAndAccount(Token.TokenType.CONFIRM_EMAIL, account.getId()).ifPresent(tokenFacade::remove);

        String tokenValue = this.jwtProvider.generateEmailToken(account, newEmail, emailChangeConfirmationPeriodLengthHours);
        Token emailToken = new Token(tokenValue, account, Token.TokenType.CONFIRM_EMAIL);
        this.tokenFacade.create(emailToken);

        String encodedTokenValue = new String(Base64.getUrlEncoder().encode(tokenValue.getBytes()));
        String confirmationURL = accountConfirmEmail + encodedTokenValue;

        mailProvider.sendEmailConfirmEmail(account.getName(), account.getLastname(), newEmail, confirmationURL, account.getAccountLanguage());
    }

    /**
     * Creates a new JWT related to changing of an account's e-mail,
     * replaces old JWT in the Token in database and sends new confirmation e-mail.
     *
     * @throws AccountNotFoundException Thrown when account from security context can't be found in the database.
     * @throws TokenNotFoundException   Thrown when there is no e-mail confirmation token related to the given account in the database.
     */
    @Override
    @RolesAllowed({Roles.AUTHENTICATED})
    public void resendEmailConfirmation() throws ApplicationBaseException {
        String login = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountFacade.findByLogin(login)
                .orElseThrow(() -> new AccountNotFoundException(I18n.ACCOUNT_NOT_FOUND_EXCEPTION));

        Token dbToken = tokenFacade.findByTypeAndAccount(Token.TokenType.CONFIRM_EMAIL, account.getId())
                .orElseThrow(() -> new TokenNotFoundException(I18n.TOKEN_NOT_FOUND_EXCEPTION));

        String newEmail = jwtProvider.extractEmail(dbToken.getTokenValue());
        String newTokenValue = jwtProvider.generateEmailToken(account, newEmail, emailChangeConfirmationPeriodLengthHours);

        String encodedTokenValue = new String(Base64.getUrlEncoder().encode(newTokenValue.getBytes()));
        String confirmationURL = accountConfirmEmail + encodedTokenValue;

        mailProvider.sendEmailConfirmEmail(account.getName(), account.getLastname(), newEmail, confirmationURL, account.getAccountLanguage());

        dbToken.setTokenValue(newTokenValue);
        tokenFacade.edit(dbToken);
    }

    /**
     * Removes the Client user level from the account.
     *
     * @param id ID of the account from which the Client user level will be removed.
     * @throws AccountNotFoundException  Threw when the account with the given ID was not found.
     * @throws AccountUserLevelException Threw when the account has no Client user level or has only one user level.
     */

    @Override
    @RolesAllowed({Roles.ADMIN})
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeClientUserLevel(String id) throws ApplicationBaseException {
        Account account = accountFacade.find(UUID.fromString(id)).orElseThrow(() -> new AccountNotFoundException(I18n.ACCOUNT_NOT_FOUND_EXCEPTION));

        if (account.getUserLevels().stream().noneMatch(userLevel -> userLevel instanceof Client)) {
            throw new AccountUserLevelException(I18n.NO_SUCH_USER_LEVEL_EXCEPTION);
        }
        if (account.getUserLevels().size() == 1) {
            throw new AccountUserLevelException(I18n.ONE_USER_LEVEL);
        }

        UserLevel clientUserLevel = account.getUserLevels().stream().filter(userLevel -> userLevel instanceof Client).findFirst().orElse(null);

        account.removeUserLevel(clientUserLevel);
        accountFacade.edit(account);

        userLevelFacade.remove(clientUserLevel);
    }

    /**
     * Removes the Staff user level from the account.
     *
     * @param id Identifier of the account from which the Staff user level will be removed.
     * @throws AccountNotFoundException  Threw when the account with the given ID was not found.
     * @throws AccountUserLevelException Threw when the account has no Staff user level or has only one user level.
     */

    @Override
    @RolesAllowed({Roles.ADMIN})
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeStaffUserLevel(String id) throws ApplicationBaseException {
        Account account = accountFacade.find(UUID.fromString(id)).orElseThrow(() -> new AccountNotFoundException(I18n.ACCOUNT_NOT_FOUND_EXCEPTION));

        if (account.getUserLevels().stream().noneMatch(userLevel -> userLevel instanceof Staff)) {
            throw new AccountUserLevelException(I18n.NO_SUCH_USER_LEVEL_EXCEPTION);
        }
        if (account.getUserLevels().size() == 1) {
            throw new AccountUserLevelException(I18n.ONE_USER_LEVEL);
        }

        UserLevel staffUserLevel = account.getUserLevels().stream().filter(userLevel -> userLevel instanceof Staff).findFirst().orElse(null);

        account.removeUserLevel(staffUserLevel);
        accountFacade.edit(account);

        userLevelFacade.remove(staffUserLevel);
    }

    /**
     * Removes the Admin user level from the account.
     *
     * @param id Identifier of the account from which the Staff user level will be removed.
     * @throws AccountNotFoundException  Threw when the account with the given ID was not found.
     * @throws AccountUserLevelException Threw when the account has no Staff user level or has only one user level.
     */

    @Override
    @RolesAllowed({Roles.ADMIN})
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeAdminUserLevel(String id) throws ApplicationBaseException {
        Account account = accountFacade.find(UUID.fromString(id)).orElseThrow(() -> new AccountNotFoundException(I18n.ACCOUNT_NOT_FOUND_EXCEPTION));

        String currentAccountLogin = SecurityContextHolder.getContext().getAuthentication().getName();
        Account currentAccount = accountFacade.findByLogin(currentAccountLogin)
                .orElseThrow(() -> new AccountNotFoundException(I18n.ACCOUNT_NOT_FOUND_EXCEPTION));

        if (account.equals(currentAccount)) {
            throw new AccountUserLevelException(I18n.ADMIN_ACCOUNT_REMOVE_OWN_ADMIN_USER_LEVEL_EXCEPTION);
        }

        if (account.getUserLevels().stream().noneMatch(userLevel -> userLevel instanceof Admin)) {
            throw new AccountUserLevelException(I18n.NO_SUCH_USER_LEVEL_EXCEPTION);
        }
        if (account.getUserLevels().size() == 1) {
            throw new AccountUserLevelException(I18n.ONE_USER_LEVEL);
        }

        UserLevel adminUserLevel = account.getUserLevels().stream().filter(userLevel -> userLevel instanceof Admin).findFirst().orElse(null);

        account.removeUserLevel(adminUserLevel);
        accountFacade.edit(account);

        userLevelFacade.remove(adminUserLevel);
    }

    /**
     * Adds the Client user level to the account.
     *
     * @param id Identifier of the account to which the Client user level will be added.
     * @throws ApplicationBaseException AccountNotFoundException - when account is not found
     *                                  AccountUserLevelException - when account already has this user level
     */
    @Override
    @RolesAllowed({Roles.ADMIN})
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = ApplicationBaseException.class)
    public void addClientUserLevel(String id) throws ApplicationBaseException {

        Account account = accountFacade.find(UUID.fromString(id)).orElseThrow(() -> new pl.lodz.p.it.ssbd2024.ssbd03.exceptions.account.read.AccountNotFoundException(I18n.ACCOUNT_NOT_FOUND_EXCEPTION));

        if (account.getUserLevels().stream().anyMatch(userLevel -> userLevel instanceof Client)) {
            throw new AccountUserLevelException(I18n.USER_LEVEL_DUPLICATED);
        }
        UserLevel clientUserLevel = new Client();
        account.addUserLevel(clientUserLevel);
        userLevelFacade.create(clientUserLevel);
        accountFacade.edit(account);
    }

    /**
     * Adds the Staff user level to the account.
     *
     * @param id Identifier of the account to which the Staff user level will be added.
     * @throws ApplicationBaseException AccountNotFoundException - when account is not found
     *                                  AccountUserLevelException - when account already has this user level
     */
    @Override
    @RolesAllowed({Roles.ADMIN})
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = ApplicationBaseException.class)
    public void addStaffUserLevel(String id) throws ApplicationBaseException {
        Account account = accountFacade.find(UUID.fromString(id)).orElseThrow(() -> new pl.lodz.p.it.ssbd2024.ssbd03.exceptions.account.read.AccountNotFoundException(I18n.ACCOUNT_NOT_FOUND_EXCEPTION));

        if (account.getUserLevels().stream().anyMatch(userLevel -> userLevel instanceof Staff)) {
            throw new AccountUserLevelException(I18n.USER_LEVEL_DUPLICATED);
        }
        UserLevel staffUserLevel = new Staff();
        account.addUserLevel(staffUserLevel);
        userLevelFacade.create(staffUserLevel);
        accountFacade.edit(account);
    }

    /**
     * Adds the Admin user level to the account.
     *
     * @param id Identifier of the account to which the Admin user level will be added.
     * @throws ApplicationBaseException AccountNotFoundException - when account is not found
     *                                  AccountUserLevelException - when account already has this user level
     */
    @Override
    @RolesAllowed({Roles.ADMIN})
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = ApplicationBaseException.class)
    public void addAdminUserLevel(String id) throws ApplicationBaseException {
        Account account = accountFacade.find(UUID.fromString(id)).orElseThrow(() -> new pl.lodz.p.it.ssbd2024.ssbd03.exceptions.account.read.AccountNotFoundException(I18n.ACCOUNT_NOT_FOUND_EXCEPTION));

        if (account.getUserLevels().stream().anyMatch(userLevel -> userLevel instanceof Admin)) {
            throw new AccountUserLevelException(I18n.USER_LEVEL_DUPLICATED);
        }
        UserLevel adminUserLevel = new Admin();
        account.addUserLevel(adminUserLevel);
        userLevelFacade.create(adminUserLevel);
        accountFacade.edit(account);
    }

    /***
     * This method is used to change own password.
     *
     * @param oldPassword The OldPassword is the old password that the user must provide for authentication.
     * @param newPassword The new password is the password that the user wants to set.
     * @param login The login retrieved from the security context.
     * @throws ApplicationBaseException - IncorrectPasswordException (when oldPassword parameter and password in database
     * are not equal), CurrentPasswordAndNewPasswordAreTheSameException (when newPassword parameter and password in database
     * are not equal). AccountNotFoundException (when account not found).
     */
    @Override
    @RolesAllowed({Roles.AUTHENTICATED})
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = ApplicationBaseException.class)
    public void changePasswordSelf(String oldPassword, String newPassword, String login) throws ApplicationBaseException {

        String newPasswordEncoded = passwordEncoder.encode(newPassword);

        Account account = accountFacade.findByLogin(login).orElseThrow(() -> new AccountNotFoundException(I18n.ACCOUNT_NOT_FOUND_EXCEPTION));

        String passwordFromDatabase = account.getPassword();

        if (!passwordEncoder.matches(oldPassword, passwordFromDatabase)) {
            throw new IncorrectPasswordException();
        }

        if (passwordEncoder.matches(newPassword, passwordFromDatabase)) {
            throw new CurrentPasswordAndNewPasswordAreTheSameException();
        }

        account.setPassword(newPasswordEncoded);
        accountFacade.edit(account);
    }
}
