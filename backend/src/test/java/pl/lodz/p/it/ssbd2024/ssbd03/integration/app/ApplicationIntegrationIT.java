package pl.lodz.p.it.ssbd2024.ssbd03.integration.app;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import pl.lodz.p.it.ssbd2024.ssbd03.TestcontainersConfigFull;
import pl.lodz.p.it.ssbd2024.ssbd03.commons.dto.mok.accountInputDTO.AccountModifyDTO;
import pl.lodz.p.it.ssbd2024.ssbd03.commons.dto.mok.accountInputDTO.AccountRegisterDTO;
import pl.lodz.p.it.ssbd2024.ssbd03.commons.dto.mok.accountOutputDTO.AccountOutputDTO;
import pl.lodz.p.it.ssbd2024.ssbd03.commons.dto.mok.authentication.AuthenticationLoginDTO;
import pl.lodz.p.it.ssbd2024.ssbd03.commons.dto.mok.exception.AccountConstraintViolationExceptionDTO;
import pl.lodz.p.it.ssbd2024.ssbd03.commons.dto.mok.token.AccessAndRefreshTokensDTO;
import pl.lodz.p.it.ssbd2024.ssbd03.commons.dto.mop.sectorDTO.SectorCreateDTO;
import pl.lodz.p.it.ssbd2024.ssbd03.commons.dto.mop.sectorDTO.SectorModifyDTO;
import pl.lodz.p.it.ssbd2024.ssbd03.commons.dto.mop.sectorDTO.SectorOutputDTO;
import pl.lodz.p.it.ssbd2024.ssbd03.utils.I18n;
import pl.lodz.p.it.ssbd2024.ssbd03.utils.consts.utils.JWTConsts;
import pl.lodz.p.it.ssbd2024.ssbd03.utils.messages.mok.AccountMessages;
import pl.lodz.p.it.ssbd2024.ssbd03.utils.messages.mop.SectorMessages;

import java.io.IOException;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ApplicationIntegrationIT extends TestcontainersConfigFull {
    
    private static final String CONTENT_TYPE = MediaType.APPLICATION_JSON_VALUE;
    private static final String BASE_URL = "http://localhost:8181/api/v1";

    private static final ObjectMapper mapper = new ObjectMapper();

    /* Enable only in case of debugging tests */
    @BeforeAll
    public static void setup() {
        RestAssured.config = RestAssured.config()
                .logConfig(LogConfig.logConfig()
                        .enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL)
                        .enablePrettyPrinting(true));
        // Enable global request and response logging filters
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
    }

    @AfterEach
    void resetEnvironment() throws IOException, InterruptedException {
        this.resetDatabase();
    }

    @Test
    public void loginUsingCredentialsEndpointTestPositive() throws Exception {
        RequestSpecification request = RestAssured.given();

        AuthenticationLoginDTO accountLoginDTO = new AuthenticationLoginDTO("jerzybem", "P@ssw0rd!", "pl");

        String adminToken = this.login(accountLoginDTO.getLogin(), accountLoginDTO.getPassword(), accountLoginDTO.getLanguage());

        AccountOutputDTO accountOutputDTOBefore = request
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .when()
                .get(BASE_URL + "/accounts/self")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(AccountOutputDTO.class);

        assertNotNull(accountOutputDTOBefore);

        request
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .when()
                .post(BASE_URL + "/auth/logout")
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value())
                .extract()
                .asString();

        adminToken = this.login(accountLoginDTO.getLogin(), accountLoginDTO.getPassword(), accountLoginDTO.getLanguage());

        AccountOutputDTO accountOutputDTOAfter = request
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .when()
                .get(BASE_URL + "/accounts/self")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(AccountOutputDTO.class);

        assertNotNull(accountOutputDTOAfter);

        assertNotNull(adminToken);

        DecodedJWT decodedJWT = JWT.decode(adminToken);

        assertEquals(accountLoginDTO.getLogin(), decodedJWT.getSubject());
        assertEquals(JWTConsts.TOKEN_ISSUER, decodedJWT.getIssuer());
        assertTrue(decodedJWT.getIssuedAt().before(new Date()));
        assertTrue(decodedJWT.getExpiresAt().after(new Date()));

        assertNotEquals(accountOutputDTOBefore.getLastSuccessfulLoginTime(), accountOutputDTOAfter.getLastUnsuccessfulLoginTime());
    }

    @Test
    public void loginUsingCredentialsEndpointTestNegativeWhenLoginIsInvalid() throws Exception {
        AuthenticationLoginDTO accountLoginDTO = new AuthenticationLoginDTO("iosif_wissarionowicz", "P@ssw0rd!", "pl");
        RequestSpecification request = RestAssured.given();

        request.contentType(CONTENT_TYPE);
        request.body(mapper.writeValueAsString(accountLoginDTO));

        request
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(mapper.writeValueAsString(accountLoginDTO))
                .when()
                .post(BASE_URL + "/auth/login-credentials")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body(
                        "message", Matchers.equalTo(I18n.INVALID_LOGIN_ATTEMPT_EXCEPTION)
                )
                .extract()
                .asString();
    }

    @Test
    public void loginUsingCredentialsEndpointTestNegativeWhenPasswordIsInvalid() throws Exception {
        AuthenticationLoginDTO accountLoginDTO = new AuthenticationLoginDTO("jerzybem", "P@ssw0rd!1", "pl");
        RequestSpecification request = RestAssured.given();

        request
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(mapper.writeValueAsString(accountLoginDTO))
                .when()
                .post(BASE_URL + "/auth/login-credentials")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body(
                        "message", Matchers.equalTo(I18n.INVALID_LOGIN_ATTEMPT_EXCEPTION)
                );
    }

    @Test
    public void loginUsingCredentialsEndpointTestNegativeWhenUserIsAlreadyLoggedIn() throws Exception {
        AuthenticationLoginDTO accountLoginDTO = new AuthenticationLoginDTO("jerzybem", "P@ssw0rd!", "pl");
        String previousToken = this.login(accountLoginDTO.getLogin(), accountLoginDTO.getPassword(), accountLoginDTO.getLanguage());
        RequestSpecification request = RestAssured.given();

        request
                .header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(previousToken))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(mapper.writeValueAsString(accountLoginDTO))
                .when()
                .post(BASE_URL + "/auth/login-credentials")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body(
                        "message", Matchers.equalTo(I18n.ACCESS_DENIED_EXCEPTION)
                );
    }

    @Test
    public void loginUsingCredentialsEndpointTestNegativeWhenUserAccountIsNotActive() throws Exception {
        AuthenticationLoginDTO accountLoginDTO = new AuthenticationLoginDTO("jchrystus", "P@ssw0rd!", "pl");
        RequestSpecification request = RestAssured.given();

        request
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(mapper.writeValueAsString(accountLoginDTO))
                .when()
                .post(BASE_URL + "/auth/login-credentials")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(
                        "message", Matchers.equalTo(I18n.ACCOUNT_INACTIVE_EXCEPTION)
                );
    }

    @Test
    public void loginUsingCredentialsEndpointTestNegativeWhenUserIsBlockedByAdmin() throws Exception {
        AuthenticationLoginDTO accountLoginDTO = new AuthenticationLoginDTO("juleswinnfield", "P@ssw0rd!", "pl");
        RequestSpecification request = RestAssured.given();

        request
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(mapper.writeValueAsString(accountLoginDTO))
                .when()
                .post(BASE_URL + "/auth/login-credentials")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(
                        "message", Matchers.equalTo(I18n.ACCOUNT_BLOCKED_BY_ADMIN)
                );
    }

    @Test
    public void loginUsingCredentialsEndpointTestNegativeWhenUserBlockedAccountByLoggingIncorrectlyTooManyTimes() throws Exception {
        AuthenticationLoginDTO accountLoginDTO = new AuthenticationLoginDTO("vincentvega", "P@ssw0rd!", "pl");
        RequestSpecification request = RestAssured.given();

        request
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(mapper.writeValueAsString(accountLoginDTO))
                .when()
                .post(BASE_URL + "/auth/login-credentials")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(
                        "message", Matchers.equalTo(I18n.ACCOUNT_BLOCKED_BY_FAILED_LOGIN_ATTEMPTS)
                );
    }

    @Test
    public void loginUsingCredentialsEndpointTestNegativeWhenLanguageIsInvalid() throws Exception {
        AuthenticationLoginDTO accountLoginDTO = new AuthenticationLoginDTO("jerzybem", "P@ssw0rd!", "SomeStringNotFollowingConstrains");
        RequestSpecification request = RestAssured.given();

        AccountConstraintViolationExceptionDTO accountConstraintViolationExceptionDTO = request
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(mapper.writeValueAsString(accountLoginDTO))
                .when()
                .post(BASE_URL + "/auth/login-credentials")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .extract()
                .as(AccountConstraintViolationExceptionDTO.class);

        assertNotNull(accountConstraintViolationExceptionDTO);
        assertEquals(I18n.ACCOUNT_CONSTRAINT_VIOLATION, accountConstraintViolationExceptionDTO.getMessage());
        //assertTrue(accountConstraintViolationExceptionDTO.getViolations().contains(AccountMessages.LANGUAGE_REGEX_NOT_MET));
        assertEquals(2, accountConstraintViolationExceptionDTO.getViolations().size());
    }

    @Test
    public void logoutTestSuccessfulLogoutAfterLogin() throws JsonProcessingException {
        String loginToken = login("jerzybem", "P@ssw0rd!", "pl");
        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .post(BASE_URL + "/auth/logout")
                .then()
                .assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    public void logoutAsUnauthenticatedUser() {
        RestAssured.given()
                .when()
                .post(BASE_URL + "/auth/logout")
                .then()
                .assertThat()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    public void logoutAfterSuccessfulLogout() throws JsonProcessingException {
        String loginToken = login("jerzybem", "P@ssw0rd!", "pl");
        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .post(BASE_URL + "/auth/logout");

        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .post(BASE_URL + "/auth/logout")
                .then()
                .assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    public void getAllUsersReturnListAndOKStatusCode() throws JsonProcessingException {
        String loginToken = login("jerzybem", "P@ssw0rd!", "pl");

        List<String> list1 = RestAssured
                .given()
                .header("Authorization", "Bearer " + loginToken)
                .param("pageNumber", 0)
                .param("pageSize", 3)
                .get(BASE_URL + "/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getList("id");

        assertEquals(3, list1.size());

        List<String> list2 = RestAssured
                .given()
                .header("Authorization", "Bearer " + loginToken)
                .param("pageNumber", 0)
                .param("pageSize", 10)
                .get(BASE_URL + "/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getList("login");

        assertEquals(list2.get(0), "aandrus");
        assertEquals(list2.get(1), "adamn");
        assertEquals(list2.get(2), "jakubkoza");
        assertEquals(list2.get(3), "jchrystus");
        assertEquals(list2.get(4), "jerzybem");
        assertEquals(list2.get(5), "juleswinnfield");
        assertEquals(list2.get(6), "kamilslimak");
        assertEquals(list2.get(7), "kwotyla");
        assertEquals(list2.get(8), "michalkowal");
        assertEquals(list2.get(9), "piotrnowak");

        List<String> list3 = RestAssured
                .given()
                .header("Authorization", "Bearer " + loginToken)
                .param("pageNumber", 1)
                .param("pageSize", 3)
                .get(BASE_URL + "/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getList("id");

        assertEquals(3, list3.size());
    }

    @Test
    public void getAllUsersReturnNoContent() throws JsonProcessingException {
        String loginToken = login("jerzybem", "P@ssw0rd!", "pl");

        RestAssured
                .given()
                .header("Authorization", "Bearer " + loginToken)
                .param("pageNumber", 4)
                .param("pageSize", 5)
                .get(BASE_URL + "/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    public void getAllUsersAsUnauthenticatedUser() {
        RestAssured
                .given()
                .param("pageNumber", 0)
                .param("pageSize", 5)
                .get(BASE_URL + "/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    public void getAllUsersAsUnauthorizedUserForbidden() throws JsonProcessingException {
        String loginToken = login("jakubkoza", "P@ssw0rd!", "pl");

        RestAssured
                .given()
                .header("Authorization", "Bearer " + loginToken)
                .param("pageNumber", 0)
                .param("pageSize", 5)
                .get(BASE_URL + "/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    public void getSelfInfoAboutAccountSuccessfulTest() throws JsonProcessingException {
        String loginToken = login("jerzybem", "P@ssw0rd!", "pl");
        List<String> list = RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .get(BASE_URL + "/accounts/self")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("login", Matchers.equalTo("jerzybem"))
                .body("name", Matchers.equalTo("Jerzy"))
                .body("email", Matchers.equalTo("jerzybem@spoko.pl"))
                .body("phoneNumber", Matchers.equalTo("111111111"))
                .body("suspended", Matchers.equalTo(false))
                .body("active", Matchers.equalTo(true))
                .body("blocked", Matchers.equalTo(false))
                .body("accountLanguage", Matchers.equalTo("pl"))
                .extract()
                .jsonPath().getList("userLevelsDto.roleName");

        assertTrue(list.contains("ADMIN"));
        assertTrue(list.contains("STAFF"));
    }

    @Test
    public void getInfoAboutAccountSuccessfulTest() throws JsonProcessingException {
        String loginToken = login("jerzybem", "P@ssw0rd!", "pl");
        List<String> list = RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .pathParam("id", "d20f860d-555a-479e-8783-67aee5b66692")
                .get(BASE_URL + "/accounts/{id}")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("login", Matchers.equalTo("adamn"))
                .body("name", Matchers.equalTo("Adam"))
                .body("email", Matchers.equalTo("adamn@example.com"))
                .body("phoneNumber", Matchers.equalTo("200000000"))
                .body("suspended", Matchers.equalTo(false))
                .body("active", Matchers.equalTo(true))
                .body("blocked", Matchers.equalTo(false))
                .body("accountLanguage", Matchers.equalTo("PL"))
                .extract()
                .jsonPath().getList("userLevelsDto.roleName");

        assertTrue(list.contains("STAFF"));
    }

    @Test
    public void getInfoABoutAccountNotFoundTest() throws JsonProcessingException {
        String loginToken = login("jerzybem", "P@ssw0rd!", "pl");
        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .pathParam("id", "d20f900d-555a-479e-8783-67aee0b66692")
                .get(BASE_URL + "/accounts/{id}")
                .then()
                .assertThat()
                .statusCode(HttpStatus.NOT_FOUND.value());

    }

    @Test
    public void getInfoAboutAccountBadRequestTest() throws JsonProcessingException {
        String loginToken = login("jerzybem", "P@ssw0rd!", "pl");
        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .pathParam("id", "ssbdtest")
                .get(BASE_URL + "/accounts/{id}")
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value());

    }

    @Test
    public void invalidPathTestNegative() throws Exception {
        String token = this.login("jerzybem", "P@ssw0rd!", "pl");

        RestAssured.given()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .when()
                .post(BASE_URL + "/not/a/real/path")
                .then()
                .assertThat()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void invalidPathAuthorized() throws JsonProcessingException {
        String loginToken = login("jerzybem", "P@ssw0rd!", "pl");
        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .post(BASE_URL + "/not/a/real/path")
                .then()
                .assertThat()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void getSelfTest() throws JsonProcessingException {
        AuthenticationLoginDTO accountLoginDTO = new AuthenticationLoginDTO("jerzybem", "P@ssw0rd!", "pl");

        RequestSpecification request = RestAssured.given();
        request.contentType(CONTENT_TYPE);
        request.body(mapper.writeValueAsString(accountLoginDTO));

        Response response = request.post(BASE_URL + "/auth/login-credentials");
        AccessAndRefreshTokensDTO accessAndRefreshTokensDTO = response.as(AccessAndRefreshTokensDTO.class);
        DecodedJWT decodedJWT = JWT.decode(accessAndRefreshTokensDTO.getAccessToken());

        assertEquals(HttpStatus.OK.value(), response.getStatusCode());
        assertEquals("jerzybem", decodedJWT.getSubject());

        request = RestAssured.given();
        request.contentType(CONTENT_TYPE);

        response = request.header("Authorization", "Bearer " + accessAndRefreshTokensDTO.getAccessToken()).get(BASE_URL + "/accounts/self");

        assertEquals(HttpStatus.OK.value(), response.getStatusCode());
    }

    // Block and unblock

    @Test
    public void blockAndUnblockAccountByAdminTestSuccessful() throws IOException {
        String loginToken = this.login("jerzybem", "P@ssw0rd!", "pl");
        RequestSpecification requestSpec = RestAssured.given()
                .header("Authorization", "Bearer " + loginToken);
        String userId = "e0bf979b-6b42-432d-8462-544d88b1ab5f";

        // Check before blocking
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .get(BASE_URL + String.format("/accounts/%s", userId))
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "id", Matchers.equalTo(userId),
                        "blocked", Matchers.equalTo(false),
                        "blockedTime", Matchers.equalTo(null)
                );

        // Block account
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .post(BASE_URL + String.format("/accounts/%s/block", userId))
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        // Check after blocking
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .get(BASE_URL + String.format("/accounts/%s", userId))
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "id", Matchers.equalTo(userId),
                        "blocked", Matchers.equalTo(true),
                        "blockedTime", Matchers.equalTo(null)
                );

        // Unblock account
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .post(BASE_URL + String.format("/accounts/%s/unblock", userId))
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        // Check after unblocking
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .get(BASE_URL + String.format("/accounts/%s", userId))
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "id", Matchers.equalTo(userId),
                        "blocked", Matchers.equalTo(false),
                        "blockedTime", Matchers.equalTo(null)
                );
    }

    // Negative blocking

    @Test
    public void blockAccountByAdminTestFailedNoLogin() {
        RequestSpecification request = RestAssured.given();
        String userId = "e0bf979b-6b42-432d-8462-544d88b1ab5f";

        // Try to block account
        request
                .when()
                .post(BASE_URL + String.format("/accounts/%s/block", userId))
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body(
                        "message", Matchers.equalTo(I18n.UNAUTHORIZED_EXCEPTION)
                );
    }

    @ParameterizedTest
    @MethodSource("provideNoAdminLevelAccountsParameters")
    public void blockAccountByAdminTestFailedNoAdminRole(String login) throws IOException {
        String loginToken = this.login(login, "P@ssw0rd!", "pl");
        RequestSpecification request = RestAssured.given();
        request.header("Authorization", "Bearer " + loginToken);
        String userId = "e0bf979b-6b42-432d-8462-544d88b1ab5f";

        // Try to block account
        request
                .when()
                .post(BASE_URL + String.format("/accounts/%s/block", userId))
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body(
                        "message", Matchers.equalTo(I18n.ACCESS_DENIED_EXCEPTION)
                );
    }

    @Test
    public void blockAccountByAdminTestFailedAccountNotFound() throws IOException {
        String loginToken = this.login("jerzybem", "P@ssw0rd!", "pl");
        RequestSpecification request = RestAssured.given();
        request.header("Authorization", "Bearer " + loginToken);
        String userId = UUID.randomUUID().toString();

        // Try to block account
        request
                .when()
                .post(BASE_URL + String.format("/accounts/%s/block", userId))
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(
                        "message", Matchers.equalTo(I18n.ACCOUNT_NOT_FOUND_EXCEPTION)
                );
    }

    @Test
    public void blockAccountByAdminTestFailedTryToBlockOwnAccount() throws IOException {
        String loginToken = this.login("jerzybem", "P@ssw0rd!", "pl");
        RequestSpecification request = RestAssured.given();
        request.header("Authorization", "Bearer " + loginToken);
        String userId = "b3b8c2ac-21ff-434b-b490-aa6d717447c0";

        // Try to block account
        request
                .when()
                .post(BASE_URL + String.format("/accounts/%s/block", userId))
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body(
                        "message", Matchers.equalTo(I18n.ACCOUNT_TRY_TO_BLOCK_OWN_EXCEPTION)
                );
    }

    @Test
    public void blockAccountByAdminTestFailedTryToBlockBlockedAccount() throws IOException {
        String loginToken = this.login("jerzybem", "P@ssw0rd!", "pl");
        RequestSpecification requestSpec = RestAssured.given()
                .header("Authorization", "Bearer " + loginToken);
        String userId = "e0bf979b-6b42-432d-8462-544d88b1ab5f";

        // Block account
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .post(BASE_URL + String.format("/accounts/%s/block", userId))
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        // Try to block account second time
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .post(BASE_URL + String.format("/accounts/%s/block", userId))
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body(
                        "message", Matchers.equalTo(I18n.ACCOUNT_ALREADY_BLOCKED)
                );

        //------------------------------------------------------------------------------------//

        // Unblock account
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .post(BASE_URL + String.format("/accounts/%s/unblock", userId))
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        // Check after unblocking
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .get(BASE_URL + String.format("/accounts/%s", userId))
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "id", Matchers.equalTo(userId),
                        "blocked", Matchers.equalTo(false),
                        "blockedTime", Matchers.equalTo(null)
                );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidUUIDParameters")
    public void blockAccountByAdminTestFailedInvalidUUID(String userId) throws IOException {
        String loginToken = this.login("jerzybem", "P@ssw0rd!", "pl");
        RequestSpecification request = RestAssured.given();
        request.header("Authorization", "Bearer " + loginToken);

        // Try to block account
        request
                .when()
                .post(BASE_URL + String.format("/accounts/%s/block", userId))
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(
                        "message", Matchers.equalTo(I18n.BAD_UUID_INVALID_FORMAT_EXCEPTION)
                );
    }

    // Negative unblocking

    @Test
    public void unblockAccountByAdminTestFailedNoLogin() {
        RequestSpecification request = RestAssured.given();
        String userId = "e0bf979b-6b42-432d-8462-544d88b1ab5f";

        // Try to unblock account
        request
                .when()
                .post(BASE_URL + String.format("/accounts/%s/unblock", userId))
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body(
                        "message", Matchers.equalTo(I18n.UNAUTHORIZED_EXCEPTION)
                );
    }

    @ParameterizedTest
    @MethodSource("provideNewUserLevelForAccountParameters")
    public void addAndRemoveUserLevelTestSuccessful(String id, String newUserLevel) throws JsonProcessingException {
        String loginToken = login("jerzybem", "P@ssw0rd!", "pl");
        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .post(BASE_URL + "/accounts/{id}/add-level-{level}", id, newUserLevel)
                .then()
                .assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());

        List<String> userLevels = RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .get(BASE_URL + "/accounts/{id}", id)
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getList("userLevelsDto.roleName");
        assertTrue(userLevels.contains(newUserLevel.toUpperCase()));


        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .post(BASE_URL + "/accounts/{id}/remove-level-{level}", id, newUserLevel)
                .then()
                .assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());

        userLevels = RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .get(BASE_URL + "/accounts/{id}", id)
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getList("userLevelsDto.roleName");

        assertFalse(userLevels.contains(newUserLevel.toUpperCase()));
    }

    @ParameterizedTest
    @MethodSource("provideOldUserLevelForAccountParameters")
    public void addUserLevelTestAccountHasUserLevel(String id, String oldUserLevel) throws JsonProcessingException {
        String loginToken = login("jerzybem", "P@ssw0rd!", "pl");
        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .post(BASE_URL + "/accounts/{id}/add-level-{level}", id, oldUserLevel)
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(
                        "message", Matchers.equalTo(I18n.USER_LEVEL_DUPLICATED)
                );
    }

    @ParameterizedTest
    @MethodSource("provideConflictingUserLevelForAccountParameters")
    public void addUserLevelTestAccountConflictingUserLevel(String id, String oldUserLevel) throws JsonProcessingException {
        String loginToken = login("jerzybem", "P@ssw0rd!", "pl");
        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .post(BASE_URL + "/accounts/{id}/add-level-{level}", id, oldUserLevel)
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(
                        "message", Matchers.equalTo(I18n.ACCOUNT_USER_LEVELS_CONFLICT)
                );
    }

    @ParameterizedTest
    @ValueSource(strings = {"client", "staff", "admin"})
    public void addUserLevelTestInvalidId(String userLevel) throws JsonProcessingException {
        String loginToken = login("jerzybem", "P@ssw0rd!", "pl");
        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .post(BASE_URL + "/accounts/invalid-id/add-level-{level}", userLevel)
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(
                        "message", Matchers.equalTo(I18n.BAD_UUID_INVALID_FORMAT_EXCEPTION)
                );
    }

    @ParameterizedTest
    @MethodSource("provideNewUserLevelForAccountParameters")
    public void addUserLevelTestUnauthorized(String id, String newUserLevel) {
        RestAssured.given()
                .when()
                .post(BASE_URL + "/accounts/{id}/add-level-{level}", id, newUserLevel)
                .then()
                .assertThat()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body(
                        "message", Matchers.equalTo(I18n.UNAUTHORIZED_EXCEPTION)
                );
    }

    @ParameterizedTest
    @MethodSource("provideNoAdminLevelAccountsParameters")
    public void unblockAccountByAdminTestFailedNoAdminRole(String login) throws IOException {
        String loginToken = this.login(login, "P@ssw0rd!", "pl");
        RequestSpecification request = RestAssured.given();
        request.header("Authorization", "Bearer " + loginToken);
        String userId = "e0bf979b-6b42-432d-8462-544d88b1ab5f";

        // Try to block account
        request
                .when()
                .post(BASE_URL + String.format("/accounts/%s/unblock", userId))
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body(
                        "message", Matchers.equalTo(I18n.ACCESS_DENIED_EXCEPTION)
                );
    }

    @Test
    public void unblockAccountByAdminTestFailedAccountNotFound() throws IOException {
        String loginToken = this.login("jerzybem", "P@ssw0rd!", "pl");
        RequestSpecification request = RestAssured.given();
        request.header("Authorization", "Bearer " + loginToken);
        String userId = UUID.randomUUID().toString();

        // Try to block account
        request
                .when()
                .post(BASE_URL + String.format("/accounts/%s/unblock", userId))
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(
                        "message", Matchers.equalTo(I18n.ACCOUNT_NOT_FOUND_EXCEPTION)
                );
    }

    @Test
    public void unblockAccountByAdminTestFailedTryToUnblockUnblockedAccount() throws IOException {
        String loginToken = this.login("jerzybem", "P@ssw0rd!", "pl");
        RequestSpecification request = RestAssured.given();
        request.header("Authorization", "Bearer " + loginToken);
        String userId = "e0bf979b-6b42-432d-8462-544d88b1ab5f";

        // Unblock account
        request
                .when()
                .post(BASE_URL + String.format("/accounts/%s/unblock", userId))
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body(
                        "message", Matchers.equalTo(I18n.ACCOUNT_ALREADY_UNBLOCKED)
                );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidUUIDParameters")
    public void unblockAccountByAdminTestFailedInvalidUUID(String userId) throws IOException {
        String loginToken = this.login("jerzybem", "P@ssw0rd!", "pl");
        RequestSpecification request = RestAssured.given();
        request.header("Authorization", "Bearer " + loginToken);

        // Try to un block account
        request
                .when()
                .post(BASE_URL + String.format("/accounts/%s/unblock", userId))
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(
                        "message", Matchers.equalTo(I18n.BAD_UUID_INVALID_FORMAT_EXCEPTION)
                );
    }

    // Modify self account

    @ParameterizedTest
    @MethodSource("provideAllLevelAccountsParameters")
    public void modifyAccountSelfTestSuccessful(String login) throws IOException {
        String loginToken = this.login(login, "P@ssw0rd!", "pl");
        RequestSpecification requestSpec = RestAssured.given()
                .header("Authorization", "Bearer " + loginToken);

        // Check before modifying
        Response responseBefore = RestAssured.given()
                .spec(requestSpec)
                .when()
                .get(BASE_URL + "/accounts/self")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "login", Matchers.equalTo(login),
                        "name", Matchers.not("Ebenezer"),
                        "phoneNumber", Matchers.not("133111222")
                )
                .extract()
                .response();

        AccountOutputDTO accountOutputDTO = responseBefore.as(AccountOutputDTO.class);

        AccountModifyDTO accountModifyDTO = toAccountModifyDTO(accountOutputDTO);
        accountModifyDTO.setName("Ebenezer");
        accountModifyDTO.setPhoneNumber("133111222");

        // Modify account
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .header("If-Match", responseBefore.getHeader("ETag").replace("\"", ""))
                .contentType(CONTENT_TYPE)
                .body(accountModifyDTO)
                .put(BASE_URL + "/accounts/self")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "login", Matchers.equalTo(login),
                        "name", Matchers.equalTo("Ebenezer"),
                        "phoneNumber", Matchers.equalTo("133111222")
                );

        // Check after modifying
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .get(BASE_URL + "/accounts/self")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "login", Matchers.equalTo(login),
                        "name", Matchers.equalTo("Ebenezer"),
                        "phoneNumber", Matchers.equalTo("133111222")
                );
    }

    // Negative modify self account

    @Test
    public void modifyAccountSelfTestFailedNoLogin() throws IOException {
        String loginToken = this.login("jerzybem", "P@ssw0rd!", "pl");

        // Check before modifying
        Response responseBefore = RestAssured.given()
                .when()
                .header("Authorization", "Bearer " + loginToken)
                .get(BASE_URL + "/accounts/self")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "login", Matchers.equalTo("jerzybem")
                )
                .extract()
                .response();

        AccountOutputDTO accountOutputDTO = responseBefore.as(AccountOutputDTO.class);
        AccountModifyDTO accountModifyDTO = toAccountModifyDTO(accountOutputDTO);
        accountModifyDTO.setName("Adam");

        // Try to modify account
        RestAssured.given()
                .when()
                .header("If-Match", responseBefore.getHeader("ETag").replace("\"", ""))
                .contentType(CONTENT_TYPE)
                .body(accountModifyDTO)
                .put(BASE_URL + "/accounts/self")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body(
                        "message", Matchers.equalTo(I18n.UNAUTHORIZED_EXCEPTION)
                );
    }

    @ParameterizedTest
    @MethodSource("provideAllLevelAccountsParameters")
    public void modifyAccountSelfTestFailedDataIntegrityCompromised(String login) throws IOException {
        String loginToken = this.login(login, "P@ssw0rd!", "pl");
        RequestSpecification requestSpec = RestAssured.given()
                .header("Authorization", "Bearer " + loginToken);

        // Check before modifying
        Response responseBefore = RestAssured.given()
                .spec(requestSpec)
                .when()
                .get(BASE_URL + "/accounts/self")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "login", Matchers.equalTo(login),
                        "name", Matchers.not("Alalalala")
                )
                .extract()
                .response();

        AccountOutputDTO accountOutputDTO = responseBefore.as(AccountOutputDTO.class);

        AccountModifyDTO accountModifyDTO = toAccountModifyDTO(accountOutputDTO);

        accountModifyDTO.setLogin("newLogin");
        accountModifyDTO.setName("Alalalala");

        // Modify account
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .header("If-Match", responseBefore.getHeader("ETag").replace("\"", ""))
                .contentType(CONTENT_TYPE)
                .body(accountModifyDTO)
                .put(BASE_URL + "/accounts/self")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(
                        "message", Matchers.equalTo(I18n.DATA_INTEGRITY_COMPROMISED)
                );

        // Check after modifying
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .get(BASE_URL + "/accounts/self")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "login", Matchers.equalTo(login),
                        "name", Matchers.not("Alalalala")
                );
    }

    @ParameterizedTest
    @MethodSource("provideAllLevelAccountsParametersAndNotValidIfMatch")
    public void modifyAccountSelfTestFailedInvalidIfMatch(String login, String ifMatch) throws IOException {
        String loginToken = this.login(login, "P@ssw0rd!", "pl");
        RequestSpecification requestSpec = RestAssured.given()
                .header("Authorization", "Bearer " + loginToken);

        // Check before modifying
        Response responseBefore = RestAssured.given()
                .spec(requestSpec)
                .when()
                .get(BASE_URL + "/accounts/self")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "login", Matchers.equalTo(login),
                        "name", Matchers.not("Alalalala")
                )
                .extract()
                .response();

        AccountOutputDTO accountOutputDTO = responseBefore.as(AccountOutputDTO.class);

        AccountModifyDTO accountModifyDTO = toAccountModifyDTO(accountOutputDTO);
        accountModifyDTO.setName("Alalalala");

        // Modify account
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .header("If-Match", ifMatch)
                .contentType(CONTENT_TYPE)
                .body(accountModifyDTO)
                .put(BASE_URL + "/accounts/self")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(
                        "message", Matchers.equalTo(I18n.MISSING_HEADER_IF_MATCH)
                );
    }

    @ParameterizedTest
    @MethodSource("provideAllLevelAccountsParameters")
    public void modifyAccountSelfTestFailedOptimisticLock(String login) throws IOException {
        String loginToken = this.login(login, "P@ssw0rd!", "pl");
        RequestSpecification requestSpec = RestAssured.given()
                .header("Authorization", "Bearer " + loginToken);

        // Get before modifying v1
        Response responseBefore_V1 = RestAssured.given()
                .spec(requestSpec)
                .when()
                .get(BASE_URL + "/accounts/self")
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(CONTENT_TYPE)
                .body(
                        "login", Matchers.equalTo(login),
                        "lastname", Matchers.not(Matchers.equalTo("Bbbbbb"))
                )
                .extract()
                .response();

        AccountOutputDTO accountOutputDTO_V1 = responseBefore_V1.as(AccountOutputDTO.class);

        AccountModifyDTO accountModifyDTO_V1 = toAccountModifyDTO(accountOutputDTO_V1);
        accountModifyDTO_V1.setLastname("Bbbbbb");

        // Get before modifying v2
        Response responseBefore_V2 = RestAssured.given()
                .spec(requestSpec)
                .when()
                .get(BASE_URL + "/accounts/self")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "login", Matchers.equalTo(login),
                        "lastname", Matchers.not(Matchers.equalTo("Bbbbbb"))
                )
                .extract()
                .response();

        AccountOutputDTO accountOutputDTO_V2 = responseBefore_V2.as(AccountOutputDTO.class);

        AccountModifyDTO accountModifyDTO_2 = toAccountModifyDTO(accountOutputDTO_V2);
        accountModifyDTO_2.setLastname("Ccccc");

        // Modify account v1
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .header("If-Match", responseBefore_V1.getHeader("ETag").replace("\"", ""))
                .contentType(CONTENT_TYPE)
                .body(accountModifyDTO_V1)
                .put(BASE_URL + "/accounts/self")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "login", Matchers.equalTo(login),
                        "lastname", Matchers.equalTo("Bbbbbb")
                );

        // Modify account v2
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .header("If-Match", responseBefore_V1.getHeader("ETag").replace("\"", ""))
                .contentType(CONTENT_TYPE)
                .body(accountModifyDTO_V1)
                .put(BASE_URL + "/accounts/self")
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body(
                        "message", Matchers.equalTo(I18n.OPTIMISTIC_LOCK_EXCEPTION)
                );

        // Check after modifying
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .get(BASE_URL + "/accounts/self")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "login", Matchers.equalTo(login),
                        "lastname", (Matchers.equalTo("Bbbbbb")
                        ));
    }

    @ParameterizedTest
    @MethodSource("provideAllLevelAccountsParameters")
    public void modifyAccountSelfTestFailedConstraintViolation(String login) throws IOException {
        String loginToken = this.login(login, "P@ssw0rd!", "pl");
        RequestSpecification requestSpec = RestAssured.given()
                .header("Authorization", "Bearer " + loginToken);

        // Check before modifying
        Response responseBefore = RestAssured.given()
                .spec(requestSpec)
                .when()
                .get(BASE_URL + "/accounts/self")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "login", Matchers.equalTo(login),
                        "name", Matchers.any(String.class)
                )
                .extract()
                .response();

        AccountOutputDTO accountOutputDTO = responseBefore.as(AccountOutputDTO.class);
        String currentName = accountOutputDTO.getName();

        AccountModifyDTO accountModifyDTO = toAccountModifyDTO(accountOutputDTO);
        accountModifyDTO.setName("A".repeat(100));

        // Modify account
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .header("If-Match", responseBefore.getHeader("ETag").replace("\"", ""))
                .contentType(CONTENT_TYPE)
                .body(accountModifyDTO)
                .put(BASE_URL + "/accounts/self")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(
                        "message", Matchers.equalTo("account.constraint.violation.exception"),
                        "violations", Matchers.hasSize(2),
                        "violations", Matchers.containsInAnyOrder(
                                Matchers.equalTo("bean.validation.account.first.name.too.long"),
                                Matchers.equalTo("bean.validation.account.first.name.regex.not.met")
                        )
                );

        // Check after modifying
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .get(BASE_URL + "/accounts/self")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "login", Matchers.equalTo(login),
                        "name", Matchers.equalTo(currentName)
                );
    }

    // Modify other user

    @Test
    public void modifyUserAccountTestSuccessful() throws IOException {
        String loginToken = this.login("jerzybem", "P@ssw0rd!", "pl");
        RequestSpecification requestSpec = RestAssured.given()
                .header("Authorization", "Bearer " + loginToken);
        String userId = "02b0d9d7-a472-48d0-95e0-13a3e6a11d00";

        // Check before modifying
        Response responseBefore = RestAssured.given()
                .spec(requestSpec)
                .when()
                .get(BASE_URL + String.format("/accounts/%s", userId))
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "login", Matchers.equalTo("piotrnowak"),
                        "name", Matchers.not("Ebenezer"),
                        "phoneNumber", Matchers.not("133111222")
                )
                .extract()
                .response();

        AccountOutputDTO accountOutputDTO = responseBefore.as(AccountOutputDTO.class);

        AccountModifyDTO accountModifyDTO = toAccountModifyDTO(accountOutputDTO);
        accountModifyDTO.setName("Ebenezer");
        accountModifyDTO.setPhoneNumber("133111222");

        // Modify account
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .header("If-Match", responseBefore.getHeader("ETag").replace("\"", ""))
                .contentType(CONTENT_TYPE)
                .body(accountModifyDTO)
                .put(BASE_URL + "/accounts")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "login", Matchers.equalTo("piotrnowak"),
                        "name", Matchers.equalTo("Ebenezer"),
                        "phoneNumber", Matchers.equalTo("133111222")
                );

        // Check after modifying
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .get(BASE_URL + String.format("/accounts/%s", userId))
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "login", Matchers.equalTo("piotrnowak"),
                        "name", Matchers.equalTo("Ebenezer"),
                        "phoneNumber", Matchers.equalTo("133111222")
                );
    }

    // Negative modify other user

    @Test
    public void modifyUserAccountTestFailedNoLogin() throws IOException {
        String loginToken = this.login("jerzybem", "P@ssw0rd!", "pl");
        String userId = "02b0d9d7-a472-48d0-95e0-13a3e6a11d00";

        // Check before modifying as Admin
        Response responseBefore = RestAssured.given()
                .when()
                .header("Authorization", "Bearer " + loginToken)
                .get(BASE_URL + String.format("/accounts/%s", userId))
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "login", Matchers.equalTo("piotrnowak")
                )
                .extract()
                .response();

        AccountOutputDTO accountOutputDTO = responseBefore.as(AccountOutputDTO.class);
        AccountModifyDTO accountModifyDTO = toAccountModifyDTO(accountOutputDTO);
        accountModifyDTO.setName("Adam");

        // Try to modify account
        RestAssured.given()
                .when()
                .header("If-Match", responseBefore.getHeader("ETag").replace("\"", ""))
                .contentType(CONTENT_TYPE)
                .body(accountModifyDTO)
                .put(BASE_URL + "/accounts")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body(
                        "message", Matchers.equalTo(I18n.UNAUTHORIZED_EXCEPTION)
                );
    }

    @ParameterizedTest
    @MethodSource("provideNoAdminLevelAccountsParameters")
    public void modifyUserAccountTestFailedNoAdminRole(String login) throws IOException {
        String loginTokenAdmin = this.login("jerzybem", "P@ssw0rd!", "pl");
        String userId = "02b0d9d7-a472-48d0-95e0-13a3e6a11d00";

        // Check before modifying as Admin
        Response responseBefore = RestAssured.given()
                .when()
                .header("Authorization", "Bearer " + loginTokenAdmin)
                .get(BASE_URL + String.format("/accounts/%s", userId))
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "login", Matchers.equalTo("piotrnowak")
                )
                .extract()
                .response();

        AccountOutputDTO accountOutputDTO = responseBefore.as(AccountOutputDTO.class);
        AccountModifyDTO accountModifyDTO = toAccountModifyDTO(accountOutputDTO);
        accountModifyDTO.setName("Adam");


        String loginTokenNoAdmin = this.login(login, "P@ssw0rd!", "pl");
        // Try to modify account
        RestAssured.given()
                .when()
                .header("Authorization", "Bearer " + loginTokenNoAdmin)
                .header("If-Match", responseBefore.getHeader("ETag").replace("\"", ""))
                .contentType(CONTENT_TYPE)
                .body(accountModifyDTO)
                .put(BASE_URL + "/accounts")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body(
                        "message", Matchers.equalTo(I18n.ACCESS_DENIED_EXCEPTION)
                );
    }

    @Test
    public void modifyUserAccountTestFailedDataIntegrityCompromised() throws IOException {
        String loginToken = this.login("jerzybem", "P@ssw0rd!", "pl");
        RequestSpecification requestSpec = RestAssured.given()
                .header("Authorization", "Bearer " + loginToken);
        String userId = "02b0d9d7-a472-48d0-95e0-13a3e6a11d00";

        // Check before modifying
        Response responseBefore = RestAssured.given()
                .spec(requestSpec)
                .when()
                .get(BASE_URL + String.format("/accounts/%s", userId))
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "login", Matchers.equalTo("piotrnowak"),
                        "name", Matchers.not("Alalalala")
                )
                .extract()
                .response();

        AccountOutputDTO accountOutputDTO = responseBefore.as(AccountOutputDTO.class);

        AccountModifyDTO accountModifyDTO = toAccountModifyDTO(accountOutputDTO);

        accountModifyDTO.setLogin("newLogin");
        accountModifyDTO.setName("Alalalala");

        // Modify account
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .header("If-Match", responseBefore.getHeader("ETag").replace("\"", ""))
                .contentType(CONTENT_TYPE)
                .body(accountModifyDTO)
                .put(BASE_URL + "/accounts")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(
                        "message", Matchers.equalTo(I18n.DATA_INTEGRITY_COMPROMISED)
                );

        // Check after modifying
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .get(BASE_URL + String.format("/accounts/%s", userId))
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "login", Matchers.equalTo("piotrnowak"),
                        "name", Matchers.not("Alalalala")
                );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    public void modifyUserAccountTestFailedInvalidIfMatch(String ifMatch) throws IOException {
        String loginToken = this.login("jerzybem", "P@ssw0rd!", "pl");
        RequestSpecification requestSpec = RestAssured.given()
                .header("Authorization", "Bearer " + loginToken);
        String userId = "02b0d9d7-a472-48d0-95e0-13a3e6a11d00";

        // Check before modifying
        Response responseBefore = RestAssured.given()
                .spec(requestSpec)
                .when()
                .get(BASE_URL + String.format("/accounts/%s", userId))
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "login", Matchers.equalTo("piotrnowak"),
                        "name", Matchers.not("Alalalala")
                )
                .extract()
                .response();

        AccountOutputDTO accountOutputDTO = responseBefore.as(AccountOutputDTO.class);

        AccountModifyDTO accountModifyDTO = toAccountModifyDTO(accountOutputDTO);
        accountModifyDTO.setName("Alalalala");

        // Modify account
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .header("If-Match", ifMatch)
                .contentType(CONTENT_TYPE)
                .body(accountModifyDTO)
                .put(BASE_URL + "/accounts")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(
                        "message", Matchers.equalTo(I18n.MISSING_HEADER_IF_MATCH)
                );
    }

    @Test
    public void modifyUserAccountTestFailedOptimisticLock() throws IOException {
        String loginToken = this.login("jerzybem", "P@ssw0rd!", "pl");
        RequestSpecification requestSpec = RestAssured.given()
                .header("Authorization", "Bearer " + loginToken);
        String userId = "02b0d9d7-a472-48d0-95e0-13a3e6a11d00";

        // Get before modifying v1
        Response responseBefore_V1 = RestAssured.given()
                .spec(requestSpec)
                .when()
                .get(BASE_URL + String.format("/accounts/%s", userId))
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(CONTENT_TYPE)
                .body(
                        "login", Matchers.equalTo("piotrnowak"),
                        "lastname", Matchers.not(Matchers.equalTo("Bbbbbb"))
                )
                .extract()
                .response();

        AccountOutputDTO accountOutputDTO_V1 = responseBefore_V1.as(AccountOutputDTO.class);

        AccountModifyDTO accountModifyDTO_V1 = toAccountModifyDTO(accountOutputDTO_V1);
        accountModifyDTO_V1.setLastname("Bbbbbb");

        // Get before modifying v2
        Response responseBefore_V2 = RestAssured.given()
                .spec(requestSpec)
                .when()
                .get(BASE_URL + String.format("/accounts/%s", userId))
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "login", Matchers.equalTo("piotrnowak"),
                        "lastname", Matchers.not(Matchers.equalTo("Bbbbbb"))
                )
                .extract()
                .response();

        AccountOutputDTO accountOutputDTO_V2 = responseBefore_V2.as(AccountOutputDTO.class);

        AccountModifyDTO accountModifyDTO_2 = toAccountModifyDTO(accountOutputDTO_V2);
        accountModifyDTO_2.setLastname("Ccccc");

        // Modify account v1
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .header("If-Match", responseBefore_V1.getHeader("ETag").replace("\"", ""))
                .contentType(CONTENT_TYPE)
                .body(accountModifyDTO_V1)
                .put(BASE_URL + "/accounts")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "login", Matchers.equalTo("piotrnowak"),
                        "lastname", Matchers.equalTo("Bbbbbb")
                );

        // Modify account v2
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .header("If-Match", responseBefore_V1.getHeader("ETag").replace("\"", ""))
                .contentType(CONTENT_TYPE)
                .body(accountModifyDTO_V1)
                .put(BASE_URL + "/accounts")
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body(
                        "message", Matchers.equalTo(I18n.OPTIMISTIC_LOCK_EXCEPTION)
                );

        // Check after modifying
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .get(BASE_URL + String.format("/accounts/%s", userId))
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "login", Matchers.equalTo("piotrnowak"),
                        "lastname", (Matchers.equalTo("Bbbbbb")
                        ));
    }

    @Test
    public void modifyUserAccountTestFailedConstraintViolation() throws IOException {
        String loginToken = this.login("jerzybem", "P@ssw0rd!", "pl");
        RequestSpecification requestSpec = RestAssured.given()
                .header("Authorization", "Bearer " + loginToken);
        String userId = "02b0d9d7-a472-48d0-95e0-13a3e6a11d00";

        // Check before modifying
        Response responseBefore = RestAssured.given()
                .spec(requestSpec)
                .when()
                .get(BASE_URL + String.format("/accounts/%s", userId))
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "login", Matchers.equalTo("piotrnowak"),
                        "name", Matchers.any(String.class)
                )
                .extract()
                .response();

        AccountOutputDTO accountOutputDTO = responseBefore.as(AccountOutputDTO.class);
        String currentName = accountOutputDTO.getName();

        AccountModifyDTO accountModifyDTO = toAccountModifyDTO(accountOutputDTO);
        accountModifyDTO.setName("A".repeat(100));

        // Modify account
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .header("If-Match", responseBefore.getHeader("ETag").replace("\"", ""))
                .contentType(CONTENT_TYPE)
                .body(accountModifyDTO)
                .put(BASE_URL + "/accounts")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(
                        "message", Matchers.equalTo("account.constraint.violation.exception"),
                        "violations", Matchers.hasSize(2),
                        "violations", Matchers.containsInAnyOrder(
                                Matchers.equalTo("bean.validation.account.first.name.too.long"),
                                Matchers.equalTo("bean.validation.account.first.name.regex.not.met")

                        )
                );

        // Check after modifying
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .get(BASE_URL + String.format("/accounts/%s", userId))
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "login", Matchers.equalTo("piotrnowak"),
                        "name", Matchers.equalTo(currentName)
                );
    }

    /*----------------------------------------------------------------------------------------------------------------*/

    @ParameterizedTest
    @ValueSource(strings = {"client", "staff", "admin"})
    public void addUserLevelTestAccountNotFound(String userLevel) throws JsonProcessingException {
        String loginToken = login("jerzybem", "P@ssw0rd!", "pl");
        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .post(BASE_URL + "/accounts/159cf8d2-4c75-4f7f-868d-adfaa6a842c0/add-level-{level}", userLevel)
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(
                        "message", Matchers.equalTo(I18n.ACCOUNT_NOT_FOUND_EXCEPTION)
                );
    }

    @ParameterizedTest
    @ValueSource(strings = {"client", "staff", "admin"})
    public void addUserLevelTestNotAdmin(String userLevel) throws JsonProcessingException {
        String loginToken = login("jakubkoza", "P@ssw0rd!", "pl");
        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .post(BASE_URL + "/accounts/f512c0b6-40b2-4bcb-8541-46077ac02101/add-level-{level}", userLevel)
                .then()
                .assertThat()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body(
                        "message", Matchers.equalTo(I18n.ACCESS_DENIED_EXCEPTION)
                );
    }

    @ParameterizedTest
    @MethodSource("provideOldUserLevelForAccountParameters")
    public void removeUserLevelTestAccountHasOneUserLevel(String id, String oldUserLevel) throws JsonProcessingException {
        String loginToken = login("jerzybem", "P@ssw0rd!", "pl");
        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .post(BASE_URL + "/accounts/{id}/remove-level-{level}", id, oldUserLevel)
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(
                        "message", Matchers.equalTo(I18n.ONE_USER_LEVEL)
                );
    }

    @ParameterizedTest
    @MethodSource("provideNewUserLevelForAccountParameters")
    public void removeUserLevelTestAccountNoSuchUserLevel(String id, String newUserLevel) throws JsonProcessingException {
        String loginToken = login("jerzybem", "P@ssw0rd!", "pl");
        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .post(BASE_URL + "/accounts/{id}/remove-level-{level}", id, newUserLevel)
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(
                        "message", Matchers.equalTo(I18n.NO_SUCH_USER_LEVEL_EXCEPTION)
                );
    }

    @Test
    public void removeUserLevelTestAdminRemovingOwnAdminUserLevel() throws JsonProcessingException {
        String loginToken = login("jerzybem", "P@ssw0rd!", "pl");
        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .post(BASE_URL + "/accounts/b3b8c2ac-21ff-434b-b490-aa6d717447c0/remove-level-admin")
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(
                        "message", Matchers.equalTo(I18n.ADMIN_ACCOUNT_REMOVE_OWN_ADMIN_USER_LEVEL_EXCEPTION)
                );
    }

    @ParameterizedTest
    @MethodSource("provideOldUserLevelForAccountParameters")
    public void removeUserLevelTestUnauthorized(String id, String oldUserLevel) {
        RestAssured.given()
                .when()
                .post(BASE_URL + "/accounts/{id}/remove-level-{level}", id, oldUserLevel)
                .then()
                .assertThat()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body(
                        "message", Matchers.equalTo(I18n.UNAUTHORIZED_EXCEPTION)
                );
    }

    @ParameterizedTest
    @ValueSource(strings = {"client", "staff", "admin"})
    public void removeUserLevelTestInvalidId(String userLevel) throws JsonProcessingException {
        String loginToken = login("jerzybem", "P@ssw0rd!", "pl");
        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .post(BASE_URL + "/accounts/invalid-id/remove-level-{level}", userLevel)
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(
                        "message", Matchers.equalTo(I18n.BAD_UUID_INVALID_FORMAT_EXCEPTION)
                );
    }

    @ParameterizedTest
    @ValueSource(strings = {"client", "staff", "admin"})
    public void removeUserLevelTestAccountNotFound(String userLevel) throws JsonProcessingException {
        String loginToken = login("jerzybem", "P@ssw0rd!", "pl");
        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .post(BASE_URL + "/accounts/159cf8d2-4c75-4f7f-868d-adfaa6a842c0/remove-level-{level}", userLevel)
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(
                        "message", Matchers.equalTo(I18n.ACCOUNT_NOT_FOUND_EXCEPTION)
                );
    }

    @ParameterizedTest
    @ValueSource(strings = {"client", "staff", "admin"})
    public void removeUserLevelTestNotAdmin(String userLevel) throws JsonProcessingException {
        String loginToken = login("jakubkoza", "P@ssw0rd!", "pl");
        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .post(BASE_URL + "/accounts/f512c0b6-40b2-4bcb-8541-46077ac02101/remove-level-{level}", userLevel)
                .then()
                .assertThat()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body(
                        "message", Matchers.equalTo(I18n.ACCESS_DENIED_EXCEPTION)
                );
    }

    @ParameterizedTest
    @ValueSource(strings = {"client", "staff", "admin"})
    public void registerByAdminTestSuccessful(String userLevel) throws JsonProcessingException {
        String loginToken = login("jerzybem", "P@ssw0rd!", "pl");
        String username = "loginsucc" + userLevel;
        String name = userLevel + "namesucc";
        String lastname = userLevel + "lastnamesucc";
        String email = userLevel + "succ@email.com";
        AccountRegisterDTO registerDTO = new AccountRegisterDTO(username, "P@ssw0rd!", name, lastname,
                email, "111111111", "pl");
        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .contentType(CONTENT_TYPE)
                .body(registerDTO)
                .post(BASE_URL + "/register/{user_level}", userLevel)
                .then()
                .assertThat()
                .statusCode(HttpStatus.CREATED.value());

        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .param("pageNumber", 0)
                .param("pageSize", 1)
                .param("phrase", name)
                .param("active", false)
                .get(BASE_URL + "/accounts/match-phrase-in-account")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("[0].id", anything())
                .body("[0].login", equalTo(username))
                .body("[0].name", equalTo(name))
                .body("[0].active", equalTo(false))
                .body("[0].blocked", equalTo(false))
                .body("[0].suspended", equalTo(false))
                .body("[0].userLevels[0]", equalTo(userLevel.substring(0, 1).toUpperCase() + userLevel.substring(1)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"client", "staff", "admin"})
    public void registerByAdminTestConstraintViolation(String userLevel) throws JsonProcessingException {
        String loginToken = login("jerzybem", "P@ssw0rd!", "pl");
        String username = "login" + userLevel;
        String name = userLevel + "name";
        String lastname = userLevel + "lastname";
        String email = userLevel + ".nobueno";
        AccountRegisterDTO registerDTO = new AccountRegisterDTO(username, "P@ssw0rd!", name, lastname,
                email, "111111111", "pl");
        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .contentType(CONTENT_TYPE)
                .body(registerDTO)
                .post(BASE_URL + "/register/{user_level}", userLevel)
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", equalTo(I18n.ACCOUNT_CONSTRAINT_VIOLATION))
                .body("violations[0]", equalTo(AccountMessages.EMAIL_CONSTRAINT_NOT_MET));
    }

    @ParameterizedTest
    @ValueSource(strings = {"client", "staff", "admin"})
    public void registerByAdminTestLoginConflict(String userLevel) throws JsonProcessingException {
        String loginToken = login("jerzybem", "P@ssw0rd!", "pl");
        String username = "jerzybem";
        String name = userLevel + "name";
        String lastname = userLevel + "lastname";
        String email = userLevel + "@email.com";
        AccountRegisterDTO registerDTO = new AccountRegisterDTO(username, "P@ssw0rd!", name, lastname,
                email, "111111111", "pl");
        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .contentType(CONTENT_TYPE)
                .body(registerDTO)
                .post(BASE_URL + "/register/{user_level}", userLevel)
                .then()
                .assertThat()
                .statusCode(HttpStatus.CONFLICT.value())
                .body(
                        "message", Matchers.equalTo(I18n.ACCOUNT_LOGIN_ALREADY_TAKEN)
                );
    }

    @ParameterizedTest
    @ValueSource(strings = {"client", "staff", "admin"})
    public void registerByAdminTestEmailConflict(String userLevel) throws JsonProcessingException {
        String loginToken = login("jerzybem", "P@ssw0rd!", "pl");
        String username = "login" + userLevel;
        String name = userLevel + "name";
        String lastname = userLevel + "lastname";
        String email = "jerzybem@spoko.pl";
        AccountRegisterDTO registerDTO = new AccountRegisterDTO(username, "P@ssw0rd!", name, lastname,
                email, "111111111", "pl");
        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .contentType(CONTENT_TYPE)
                .body(registerDTO)
                .post(BASE_URL + "/register/{user_level}", userLevel)
                .then()
                .assertThat()
                .statusCode(HttpStatus.CONFLICT.value())
                .body(
                        "message", Matchers.equalTo(I18n.ACCOUNT_EMAIL_ALREADY_TAKEN)
                );
    }

    @ParameterizedTest
    @ValueSource(strings = {"client", "staff", "admin"})
    public void registerByUnauthorizedUserTest(String userLevel) throws JsonProcessingException {
        String loginToken = login("jakubkoza", "P@ssw0rd!", "pl");
        String username = "login" + userLevel;
        String name = userLevel + "name";
        String lastname = userLevel + "lastname";
        String email = userLevel + "@email.com";
        AccountRegisterDTO registerDTO = new AccountRegisterDTO(username, "P@ssw0rd!", name, lastname,
                email, "111111111", "pl");
        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .contentType(CONTENT_TYPE)
                .body(registerDTO)
                .post(BASE_URL + "/register/{user_level}", userLevel)
                .then()
                .assertThat()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body(
                        "message", Matchers.equalTo(I18n.ACCESS_DENIED_EXCEPTION)
                );

    }

    @Test
    public void registerClientByAnonymousTestSuccessful() throws JsonProcessingException {
        String loginToken = login("jerzybem", "P@ssw0rd!", "pl");
        String username = "veryUniqueLoginOne";
        String name = "VeryQoolName";
        String lastname = "VeryQoolLastname";
        String email = "veryunique@email.com";
        AccountRegisterDTO registerDTO = new AccountRegisterDTO(username, "P@ssw0rd!", name, lastname,
                email, "111111111", "pl");
        RestAssured.given()
                .when()
                .contentType(CONTENT_TYPE)
                .body(registerDTO)
                .post(BASE_URL + "/register/client")
                .then()
                .assertThat()
                .statusCode(HttpStatus.CREATED.value());

        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .param("pageNumber", 0)
                .param("pageSize", 1)
                .param("phrase", name)
                .param("active", false)
                .get(BASE_URL + "/accounts/match-phrase-in-account")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("[0].id", anything())
                .body("[0].login", equalTo(username))
                .body("[0].name", equalTo(name))
                .body("[0].active", equalTo(false))
                .body("[0].blocked", equalTo(false))
                .body("[0].suspended", equalTo(false))
                .body("[0].userLevels[0]", equalTo("Client"));
    }

    @Test
    public void registerByAnonymousTestConstraintViolation() {
        String username = "veryUniqueLogin";
        String name = "VeryQoolName";
        String lastname = "VeryQoolLastname";
        String email = "veryuniqueemail.com";
        AccountRegisterDTO registerDTO = new AccountRegisterDTO(username, "P@ssw0rd!", name, lastname,
                email, "111111111", "pl");
        RestAssured.given()
                .when()
                .contentType(CONTENT_TYPE)
                .body(registerDTO)
                .post(BASE_URL + "/register/client")
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", equalTo(I18n.ACCOUNT_CONSTRAINT_VIOLATION))
                .body("violations[0]", equalTo(AccountMessages.EMAIL_CONSTRAINT_NOT_MET));
    }

    @Test
    public void registerByAnonymousTestLoginConflict() {
        String username = "jerzybem";
        String name = "VeryQoolName";
        String lastname = "VeryQoolLastname";
        String email = "veryunique@email.com";
        AccountRegisterDTO registerDTO = new AccountRegisterDTO(username, "P@ssw0rd!", name, lastname,
                email, "111111111", "pl");
        RestAssured.given()
                .when()
                .contentType(CONTENT_TYPE)
                .body(registerDTO)
                .post(BASE_URL + "/register/client")
                .then()
                .assertThat()
                .statusCode(HttpStatus.CONFLICT.value())
                .body(
                        "message", Matchers.equalTo(I18n.ACCOUNT_LOGIN_ALREADY_TAKEN)
                );
    }

    @Test
    public void registerByAnonymousTestEmailConflict() {
        String username = "veryUniqueLogin";
        String name = "VeryQoolName";
        String lastname = "VeryQoolLastname";
        String email = "jerzybem@spoko.pl";
        AccountRegisterDTO registerDTO = new AccountRegisterDTO(username, "P@ssw0rd!", name, lastname,
                email, "111111111", "pl");
        RestAssured.given()
                .header("Authorization", "Bearer ")
                .when()
                .contentType(CONTENT_TYPE)
                .body(registerDTO)
                .post(BASE_URL + "/register/client")
                .then()
                .assertThat()
                .statusCode(HttpStatus.CONFLICT.value())
                .body(
                        "message", Matchers.equalTo(I18n.ACCOUNT_EMAIL_ALREADY_TAKEN)
                );
    }

    @Test
    public void getAllSectorsByParkingIdReturnListAndOKStatusCode() throws JsonProcessingException {
        String loginToken = login("tkarol", "P@ssw0rd!", "pl");

        List<String> sectors =
                RestAssured
                .given()
                .header("Authorization", "Bearer " + loginToken)
                .param("pageNumber", 0)
                .param("pageSize", 3)
                .pathParam("id","96a36faa-f2a2-41b8-9c3c-b6bef04ce6d1")
                .get(BASE_URL + "/parking/{id}/sectors")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                        .body("[0].id", anything())
                        .body("[0].name", anything())
                        .body("[0].weight", anything())
                        .body("[0].max_places", anything())
                        .body("[0].active", anything())
                        .extract()
                        .jsonPath()
                        .getList("id");

        assertFalse(sectors.isEmpty());
        assertEquals(3, sectors.size());

    }

    @Test
    public void getAllSectorsByParkingIdReturnNoContent() throws JsonProcessingException {
        String loginToken = login("tkarol", "P@ssw0rd!", "pl");

        RestAssured
                .given()
                .header("Authorization", "Bearer " + loginToken)
                .param("pageNumber", 0)
                .param("pageSize", 3)
                .pathParam("id","ddcae4ec-aeb5-4ece-aa2b-46819763d55f")
                .get(BASE_URL + "/parking/{id}/sectors")
                .then()
                .assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    public void getAllSectorsByParkingIdAsUnauthenticatedUser() {
        RestAssured
                .given()
                .param("pageNumber", 0)
                .param("pageSize", 5)
                .pathParam("id","96a36faa-f2a2-41b8-9c3c-b6bef04ce6d1")
                .get(BASE_URL + "/parking/{id}/sectors")
                .then()
                .assertThat()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("message", Matchers.equalTo(I18n.UNAUTHORIZED_EXCEPTION));
    }

    @Test
    public void getAllSectorsByParkingIdAsUnauthorizedUserForbidden() throws JsonProcessingException {
        String loginToken = login("jakubkoza", "P@ssw0rd!", "pl");

        RestAssured
                .given()
                .header("Authorization", "Bearer " + loginToken)
                .param("pageNumber", 0)
                .param("pageSize", 5)
                .pathParam("id","96a36faa-f2a2-41b8-9c3c-b6bef04ce6d1")
                .get(BASE_URL + "/parking/{id}/sectors")
                .then()
                .assertThat()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("message", Matchers.equalTo(I18n.ACCESS_DENIED_EXCEPTION));
    }

    @Test
    public void getAllSectorsByParkingIdListAsAuthenticatedAndUnauthorizedUserRequestWithoutParameters()
            throws JsonProcessingException {

        String loginToken = login("tkarol", "P@ssw0rd!", "pl");
        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .pathParam("id","66")
                .get(BASE_URL + "/parking/{id}/sectors")
                .then()
                .assertThat()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("message", Matchers.equalTo(I18n.INTERNAL_SERVER_ERROR));
    }

    @Test
    public void getAllSectorsByParkingIdListWithInvalidParameters() throws JsonProcessingException {
        String loginToken = login("tkarol", "P@ssw0rd!", "pl");
        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .param("pageNumber", "invalid")
                .param("pageSize", 10)
                .pathParam("id","96a36faa-f2a2-41b8-9c3c-b6bef04ce6d1")
                .get(BASE_URL + "/parking/{id}/sectors")
                .then()
                .assertThat()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("message", Matchers.equalTo(I18n.INTERNAL_SERVER_ERROR));
    }



    @Test
    public void getAllActiveReservationsAsAuthenticatedAndAuthorizedUserReturnNoContent() throws JsonProcessingException {
        String loginToken = login("piotrnowak", "P@ssw0rd!", "pl");

        RestAssured
                .given()
                .header("Authorization", "Bearer " + loginToken)
                .param("pageNumber", 0)
                .param("pageSize", 3)
                .get(BASE_URL + "/reservations/active/self")
                .then()
                .assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    public void getAllActiveReservationsAsAuthenticatedAndAuthorizedUserWithoutParameters() throws JsonProcessingException {
        String loginTokenNo1 = login("jakubkoza", "P@ssw0rd!", "pl");
        RestAssured.given()
                .header("Authorization", "Bearer " + loginTokenNo1)
                .when()
                .get(BASE_URL + "/reservations/active/self")
                .then()
                .assertThat()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("message", Matchers.equalTo(I18n.INTERNAL_SERVER_ERROR));
    }

    @Test
    public void getAllActiveReservationsAsAuthenticatedAndAuthorizedUserWithInvalidParameters() throws JsonProcessingException {
        String loginTokenNo1 = login("jakubkoza", "P@ssw0rd!", "pl");
        RestAssured.given()
                .header("Authorization", "Bearer " + loginTokenNo1)
                .when()
                .param("pageNumber", "invalid")
                .param("pageSize", 10)
                .get(BASE_URL + "/reservations/active/self")
                .then()
                .assertThat()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("message", Matchers.equalTo(I18n.INTERNAL_SERVER_ERROR));
    }

    @Test
    public void getAllActiveReservationsAsUnauthorizedUserForbidden() throws JsonProcessingException {
        String loginToken = login("tkarol", "P@ssw0rd!", "pl");

        RestAssured
                .given()
                .header("Authorization", "Bearer " + loginToken)
                .param("pageNumber", 0)
                .param("pageSize", 5)
                .get(BASE_URL + "/reservations/active/self")
                .then()
                .assertThat()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("message", Matchers.equalTo(I18n.ACCESS_DENIED_EXCEPTION));


    }

    @Test
    public void getAllActiveReservationsAsUnauthenticatedUser() throws JsonProcessingException {
        RestAssured
                .given()
                .param("pageNumber", 0)
                .param("pageSize", 5)
                .get(BASE_URL + "/reservations/active/self")
                .then()
                .assertThat()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("message", Matchers.equalTo(I18n.UNAUTHORIZED_EXCEPTION));
    }

    @Test
    public void getAllActiveReservationsReturnListAndOKStatusCode() throws JsonProcessingException {
        String loginToken = login("jakubkoza", "P@ssw0rd!", "pl");

        List<String> reservations =
                RestAssured
                        .given()
                        .header("Authorization", "Bearer " + loginToken)
                        .param("pageNumber", 0)
                        .param("pageSize", 3)
                        .get(BASE_URL + "/reservations/active/self")
                        .then()
                        .assertThat()
                        .statusCode(HttpStatus.OK.value())
                        .extract()
                        .jsonPath()
                        .getList("id");

        assertFalse(reservations.isEmpty());

        assertEquals(2, reservations.size());

    }

    @Test
    public void addNewSectorSuccessful() throws JsonProcessingException {
        String loginToken = login("tkarol", "P@ssw0rd!", "pl");

        String name = "SA-11";
        int maxPlaces = 100;
        int weight = 100;
        boolean active = true;

        SectorCreateDTO sectorCreateDTO = new SectorCreateDTO(name, "UNCOVERED",maxPlaces,weight,active);


        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .contentType(CONTENT_TYPE)
                .body(sectorCreateDTO)
                .pathParam("id","96a36faa-f2a2-41b8-9c3c-b6bef04ce6d1")
                .post(BASE_URL + "/parking/{id}/sectors")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value());
}

    @Test
    public void addNewSectorAsUnauthorizedUserForbidden() throws JsonProcessingException {
        String loginToken = login("jakubkoza", "P@ssw0rd!", "pl");

        String name = "SA-11";
        int maxPlaces = 100;
        int weight = 100;
        boolean active = true;

        SectorCreateDTO sectorCreateDTO = new SectorCreateDTO(name, "UNCOVERED",maxPlaces,weight,active);

        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .contentType(CONTENT_TYPE)
                .body(sectorCreateDTO)
                .pathParam("id","96a36faa-f2a2-41b8-9c3c-b6bef04ce6d1")
                .post(BASE_URL + "/parking/{id}/sectors")
                .then()
                .assertThat()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("message", Matchers.equalTo(I18n.ACCESS_DENIED_EXCEPTION));
    }

    @Test
    public void addSectorAsAuthenticatedAndAuthorizedNoRequestBody() throws JsonProcessingException {
        String loginToken = login("tkarol", "P@ssw0rd!", "pl");
        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .contentType(CONTENT_TYPE)
                .pathParam("id","96a36faa-f2a2-41b8-9c3c-b6bef04ce6d1")
                .post(BASE_URL + "/parking/{id}/sectors")
                .then()
                .assertThat()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body("message", Matchers.equalTo(I18n.INTERNAL_SERVER_ERROR));
    }

    @Test
    public void addSectorAsAuthenticatedAndAuthorizedRequestBodyBlankName() throws JsonProcessingException {
        String loginToken = login("tkarol", "P@ssw0rd!", "pl");
        String name = "";
        int maxPlaces = 100;
        int weight = 100;
        boolean active = true;

        SectorCreateDTO sectorCreateDTO = new SectorCreateDTO(name, "UNCOVERED",maxPlaces,weight,active);

        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .contentType(CONTENT_TYPE)
                .body(mapper.writeValueAsString(sectorCreateDTO))
                .when()
                .pathParam("id","96a36faa-f2a2-41b8-9c3c-b6bef04ce6d1")
                .post(BASE_URL + "/parking/{id}/sectors")
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("violations[0]", Matchers.equalTo(SectorMessages.SECTOR_NAME_BLANK));
    }

    @Test
    public void addNewSectorConflict() throws JsonProcessingException {
        String loginToken = login("tkarol", "P@ssw0rd!", "pl");

        String name = "SA-01";
        int maxPlaces = 100;
        int weight = 100;
        boolean active = true;

        SectorCreateDTO sectorCreateDTO = new SectorCreateDTO(name, "UNCOVERED",maxPlaces,weight,active);

        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .contentType(CONTENT_TYPE)
                .body(sectorCreateDTO)
                .pathParam("id","96a36faa-f2a2-41b8-9c3c-b6bef04ce6d1")
                .post(BASE_URL + "/parking/{id}/sectors")
                .then()
                .assertThat()
                .statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    public void addNewSectorAsUnauthenticatedUser() throws JsonProcessingException {
        String name = "SA-01";
        int maxPlaces = 100;
        int weight = 100;
        boolean active = true;

        SectorCreateDTO sectorCreateDTO = new SectorCreateDTO(name, "UNCOVERED",maxPlaces,weight,active);

        RestAssured.given()
                .when()
                .contentType(CONTENT_TYPE)
                .body(sectorCreateDTO)
                .pathParam("id","96a36faa-f2a2-41b8-9c3c-b6bef04ce6d1")
                .post(BASE_URL + "/parking/{id}/sectors")
                .then()
                .assertThat()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("message", Matchers.equalTo(I18n.UNAUTHORIZED_EXCEPTION));
    }

    @Test
    public void addNewSectorBadRequestWeightTooLarge() throws JsonProcessingException {
        String loginToken = login("jakubkoza", "P@ssw0rd!", "pl");

        String name = "SA-11";
        int maxPlaces = 100;
        int weight = 1000;
        boolean active = true;

        SectorCreateDTO sectorCreateDTO = new SectorCreateDTO(name, "UNCOVERED",maxPlaces,weight,active);

        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .contentType(CONTENT_TYPE)
                .body(sectorCreateDTO)
                .pathParam("id","96a36faa-f2a2-41b8-9c3c-b6bef04ce6d1")
                .post(BASE_URL + "/parking/{id}/sectors")
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void addNewSectorBadRequestOverTheLimitOfMaxPlaces() throws JsonProcessingException {
        String loginToken = login("jakubkoza", "P@ssw0rd!", "pl");

        String name = "SA-11";
        int maxPlaces = 2000;
        int weight = 100;
        boolean active = true;

        SectorCreateDTO sectorCreateDTO = new SectorCreateDTO(name, "UNCOVERED",maxPlaces,weight,active);

        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .contentType(CONTENT_TYPE)
                .body(sectorCreateDTO)
                .pathParam("id","96a36faa-f2a2-41b8-9c3c-b6bef04ce6d1")
                .post(BASE_URL + "/parking/{id}/sectors")
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void addNewSectorParkingNotFound() throws JsonProcessingException {
        String loginToken = login("tkarol", "P@ssw0rd!", "pl");

        String name = "SA-11";
        int maxPlaces = 100;
        int weight = 100;
        boolean active = true;

        SectorCreateDTO sectorCreateDTO = new SectorCreateDTO(name, "UNCOVERED",maxPlaces,weight,active);


        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .contentType(CONTENT_TYPE)
                .body(sectorCreateDTO)
                .pathParam("id","96f36faa-f2a2-41b8-9c3c-b6bef04ce6d1")
                .post(BASE_URL + "/parking/{id}/sectors")
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void editSectorTestInvalidName() throws JsonProcessingException {
        String loginToken = this.login("tkarol", "P@ssw0rd!", "pl");
        RequestSpecification requestSpec = RestAssured.given()
                .header("Authorization", "Bearer " + loginToken);


        Response responseBefore = RestAssured.given()
                .spec(requestSpec)
                .when()
                .pathParam("id", "3e6a85db-d751-4549-bbb7-9705f0b2fa6b")
                .get(BASE_URL + "/parking/sectors/get/{id}")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "name", Matchers.equalTo("SA-01"),
                        "type", Matchers.equalTo("UNCOVERED"),
                        "maxPlaces", Matchers.not(40),
                        "weight", Matchers.equalTo(1)
                )
                .extract()
                .response();

        SectorOutputDTO sectorOutputDTO = responseBefore.as(SectorOutputDTO.class);

        SectorModifyDTO sectorModifyDTO = toSectorModifyDTO(sectorOutputDTO);
        sectorModifyDTO.setName("ABCDEFGH");


        RestAssured.given()
                .spec(requestSpec)
                .when()
                .header("If-Match", responseBefore.getHeader("ETag").replace("\"", ""))
                .contentType(CONTENT_TYPE)
                .body(sectorModifyDTO)
                .put(BASE_URL + "/parking/sectors")
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body(
                        "weight", Matchers.not("ABCDEFGH")
                );

        RestAssured.given()
                .spec(requestSpec)
                .when()
                .pathParam("id","3e6a85db-d751-4549-bbb7-9705f0b2fa6b")
                .get(BASE_URL + "/parking/sectors/get/{id}")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "name", Matchers.equalTo("SA-01")
                );
    }

    @Test
    public void editSectorTestSuccessful() throws JsonProcessingException {
        String loginToken = this.login("tkarol", "P@ssw0rd!", "pl");
        RequestSpecification requestSpec = RestAssured.given()
                .header("Authorization", "Bearer " + loginToken);


        Response responseBefore = RestAssured.given()
                .spec(requestSpec)
                .when()
                .pathParam("id", "3e6a85db-d751-4549-bbb7-9705f0b2fa6b")
                .get(BASE_URL + "/parking/sectors/get/{id}")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "name", Matchers.equalTo("SA-01"),
                        "type", Matchers.equalTo("UNCOVERED"),
                        "maxPlaces", Matchers.not(40),
                        "weight", Matchers.equalTo(1)
                )
                .extract()
                .response();

        SectorOutputDTO sectorOutputDTO = responseBefore.as(SectorOutputDTO.class);

        SectorModifyDTO sectorModifyDTO = toSectorModifyDTO(sectorOutputDTO);
        sectorModifyDTO.setWeight(2);


        RestAssured.given()
                .spec(requestSpec)
                .when()
                .header("If-Match", responseBefore.getHeader("ETag").replace("\"", ""))
                .contentType(CONTENT_TYPE)
                .body(sectorModifyDTO)
                .put(BASE_URL + "/parking/sectors")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "weight", Matchers.equalTo(2)
                );

        RestAssured.given()
                .spec(requestSpec)
                .when()
                .pathParam("id","3e6a85db-d751-4549-bbb7-9705f0b2fa6b")
                .get(BASE_URL + "/parking/sectors/get/{id}")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "weight", Matchers.equalTo(2)
                );
    }

        @Test
        public void editSectorTestFailedDataIntegrityCompromised() throws JsonProcessingException {
            String loginToken = login("tkarol", "P@ssw0rd!", "pl");
            RequestSpecification requestSpec = RestAssured.given()
                    .header("Authorization", "Bearer " + loginToken);


            Response responseBefore = RestAssured.given()
                    .spec(requestSpec)
                    .when()
                    .pathParam("id", "3e6a85db-d751-4549-bbb7-9705f0b2fa6b")
                    .get(BASE_URL + "/parking/sectors/get/{id}")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body(
                            "name", Matchers.equalTo("SA-01"),
                            "weight", Matchers.equalTo(1)
                    )
                    .extract()
                    .response();

            SectorOutputDTO sectorOutputDTO = responseBefore.as(SectorOutputDTO.class);
            SectorModifyDTO sectorModifyDTO = toSectorModifyDTO(sectorOutputDTO);

            sectorModifyDTO.setName("SSSSSSSSS");
            sectorModifyDTO.setWeight(100000);


            RestAssured.given()
                    .spec(requestSpec)
                    .when()
                    .header("If-Match", responseBefore.getHeader("ETag").replace("\"", ""))
                    .contentType(CONTENT_TYPE)
                    .body(sectorModifyDTO)
                    .put(BASE_URL + "/parking/sectors")
                    .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value());
            ;

            Response responseAfter = RestAssured.given()
                    .spec(requestSpec)
                    .when()
                    .pathParam("id", "3e6a85db-d751-4549-bbb7-9705f0b2fa6b")
                    .get(BASE_URL + "/parking/sectors/get/{id}")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body(
                            "name", Matchers.equalTo("SA-01"),
                            "weight", Matchers.not(100000)
                    )
                    .extract()
                    .response();
        }


    @Test
    public void editSectorTestFailedInvalidIfMatch() throws JsonProcessingException {
        String loginToken = login("tkarol", "P@ssw0rd!", "pl");
        RequestSpecification requestSpec = RestAssured.given()
                .header("Authorization", "Bearer " + loginToken);


        Response responseBefore = RestAssured.given()
                .spec(requestSpec)
                .when()
                .pathParam("id", "3e6a85db-d751-4549-bbb7-9705f0b2fa6b")
                .get(BASE_URL + "/parking/sectors/get/{id}")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "name", Matchers.equalTo("SA-01"),
                        "weight", Matchers.equalTo(1)
                )
                .extract()
                .response();

        SectorOutputDTO sectorOutputDTO = responseBefore.as(SectorOutputDTO.class);
        SectorModifyDTO sectorModifyDTO = toSectorModifyDTO(sectorOutputDTO);
        sectorModifyDTO.setWeight(5);


        RestAssured.given()
                .spec(requestSpec)
                .when()
                .header("If-Match", "")
                .contentType(CONTENT_TYPE)
                .body(sectorModifyDTO)
                .put(BASE_URL + "/parking/sectors")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(
                        "message", Matchers.equalTo(I18n.MISSING_HEADER_IF_MATCH)
                );
    }

    @Test
    public void editSectorTestFailedOptimisticLock() throws JsonProcessingException {
        String loginToken = this.login("tkarol", "P@ssw0rd!", "pl");
        RequestSpecification requestSpec = RestAssured.given()
                .header("Authorization", "Bearer " + loginToken);


        Response responseBefore_V1 = RestAssured.given()
                .spec(requestSpec)
                .when()
                .pathParam("id", "3e6a85db-d751-4549-bbb7-9705f0b2fa6b")
                .get(BASE_URL + "/parking/sectors/get/{id}")
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(CONTENT_TYPE)
                .body(
                        "name", Matchers.equalTo("SA-01"),
                        "weight", Matchers.equalTo(1)
                )
                .extract()
                .response();

        SectorOutputDTO sectorOutputDTO_V1 = responseBefore_V1.as(SectorOutputDTO.class);
        SectorModifyDTO sectorModifyDTO_V1 = toSectorModifyDTO(sectorOutputDTO_V1);

        sectorModifyDTO_V1.setWeight(5);


        Response responseBefore_V2 = RestAssured.given()
                .spec(requestSpec)
                .when()
                .pathParam("id", "3e6a85db-d751-4549-bbb7-9705f0b2fa6b")
                .get(BASE_URL + "/parking/sectors/get/{id}")
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(CONTENT_TYPE)
                .body(
                        "name", Matchers.equalTo("SA-01"),
                        "weight", Matchers.equalTo(1)
                )
                .extract()
                .response();

        SectorOutputDTO sectorOutputDTO_V2 = responseBefore_V1.as(SectorOutputDTO.class);
        SectorModifyDTO sectorModifyDTO_V2 = toSectorModifyDTO(sectorOutputDTO_V1);

        sectorModifyDTO_V1.setWeight(5);


        RestAssured.given()
                .spec(requestSpec)
                .when()
                .header("If-Match", responseBefore_V1.getHeader("ETag").replace("\"", ""))
                .contentType(CONTENT_TYPE)
                .body(sectorModifyDTO_V1)
                .put(BASE_URL + "/parking/sectors")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "name", Matchers.equalTo("SA-01"),
                        "weight", Matchers.equalTo(5)
                );


        RestAssured.given()
                .spec(requestSpec)
                .when()
                .header("If-Match", responseBefore_V1.getHeader("ETag").replace("\"", ""))
                .contentType(CONTENT_TYPE)
                .body(sectorModifyDTO_V2)
                .put(BASE_URL + "/parking/sectors")
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body(
                        "message", Matchers.equalTo(I18n.OPTIMISTIC_LOCK_EXCEPTION)
                );


        RestAssured.given()
                .spec(requestSpec)
                .when()
                .pathParam("id", "3e6a85db-d751-4549-bbb7-9705f0b2fa6b")
                .get(BASE_URL + "/parking/sectors/get/{id}")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(
                        "name", Matchers.equalTo("SA-01"),
                        "weight", Matchers.equalTo(5)
                        );
    }

    @Test
    public void editSectorAsAuthenticatedAndAuthorizedUserParkingDoesNotExist() throws JsonProcessingException {
        String loginTokenNo1 = login("tkarol", "P@ssw0rd!", "pl");
        RequestSpecification requestSpec = RestAssured.given()
                .header("Authorization", "Bearer " + loginTokenNo1);

        Response response = RestAssured.given()
                .spec(requestSpec)
                .when()
                .pathParam("id", "bca50310-f4fb-4911-bf3c-68e00e517b95")
                .get(BASE_URL + "/parking/sectors/get/{id}")
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(CONTENT_TYPE)
                .extract()
                .response();

        SectorOutputDTO sectorOutputDTO_V1 = response.as(SectorOutputDTO.class);
        SectorModifyDTO sectorModifyDTO_V1 = toSectorModifyDTO(sectorOutputDTO_V1);

        sectorModifyDTO_V1.setWeight(5);

        RestAssured.given()
                .header("Authorization", "Bearer " + loginTokenNo1)
                .when()
                .pathParam("id", "bca50310-f4fb-4911-bf3c-68e00e517b95")
                .delete(BASE_URL + "/parking/sectors/{id}")
                .then()
                .assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());

        //Modify as unauthenticated
        RestAssured.given()
                .spec(requestSpec)
                .when()
                .header("If-Match", response.getHeader("ETag").replace("\"", ""))
                .contentType(CONTENT_TYPE)
                .body(sectorModifyDTO_V1)
                .put(BASE_URL + "/parking/sectors")
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", Matchers.equalTo(I18n.SECTOR_NOT_FOUND));
    }

    @Test
    public void removeSectorTestSuccessful() throws JsonProcessingException {
        String loginToken = this.login("tkarol", "P@ssw0rd!", "pl");
        RequestSpecification requestSpec = RestAssured.given()
                .header("Authorization", "Bearer " + loginToken);


        Response responseBefore= RestAssured.given()
                .spec(requestSpec)
                .when()
                .pathParam("id", "bca50310-f4fb-4911-bf3c-68e00e517b95")
                .get(BASE_URL + "/parking/sectors/get/{id}")
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(CONTENT_TYPE)
                .body(
                        "name", Matchers.equalTo("SB-03"),
                        "weight", Matchers.equalTo(1)
                )
                .extract()
                .response();

        RestAssured.given()
                .spec(requestSpec)
                .when()
                .contentType(CONTENT_TYPE)
                .pathParam("id", "bca50310-f4fb-4911-bf3c-68e00e517b95")
                .delete(BASE_URL + "/parking/sectors/{id}")
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

    }

    @Test
    public void removeSectorAsUnauthenticatedUser() throws JsonProcessingException {
        RestAssured.given()
                .when()
                .pathParam("id", "3e6a85db-d751-4549-bbb7-9705f0b2fa6b")
                .delete(BASE_URL + "/parking/sectors/{id}")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("message", Matchers.equalTo(I18n.UNAUTHORIZED_EXCEPTION));
    }

    @Test
    public void removeSectorNotFound() throws JsonProcessingException {
        String loginToken = login("tkarol", "P@ssw0rd!", "pl");

        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .contentType(CONTENT_TYPE)
                .pathParam("id","96f36faa-f2a2-41b8-9c3c-b6bef04ce6d1")
                .delete(BASE_URL + "/parking/sectors/{id}")
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", Matchers.equalTo(I18n.SECTOR_NOT_FOUND));
    }

    @Test
    public void removeSectorEmptySectorId() throws JsonProcessingException {
        String loginToken = login("tkarol", "P@ssw0rd!", "pl");

        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .contentType(CONTENT_TYPE)
                .pathParam("id","")
                .delete(BASE_URL + "/parking/sectors/{id}")
                .then()
                .assertThat()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("message", Matchers.equalTo(I18n.PATH_NOT_FOUND_EXCEPTION));
    }

    @Test
    public void removeSectorInvalidSectorId() throws JsonProcessingException {
        String loginToken = login("tkarol", "P@ssw0rd!", "pl");

        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .contentType(CONTENT_TYPE)
                .pathParam("id","12345")
                .delete(BASE_URL + "/parking/sectors/{id}")
                .then()
                .assertThat()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", Matchers.equalTo(I18n.BAD_UUID_INVALID_FORMAT_EXCEPTION));
    }

    @Test
    public void removeSectorAsUnauthorizedUserForbidden() throws JsonProcessingException {
        String loginToken = login("jakubkoza", "P@ssw0rd!", "pl");

        RestAssured.given()
                .header("Authorization", "Bearer " + loginToken)
                .when()
                .contentType(CONTENT_TYPE)
                .pathParam("id","96a36faa-f2a2-41b8-9c3c-b6bef04ce6d1")
                .delete(BASE_URL + "/parking/sectors/{id}")
                .then()
                .assertThat()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("message", Matchers.equalTo(I18n.ACCESS_DENIED_EXCEPTION));
    }




    private static Stream<Arguments> provideNewUserLevelForAccountParameters() {
        return Stream.of(
                Arguments.of("9a333f13-5ccc-4109-bce3-0ad629843edf", "staff"), //aandrus
                Arguments.of("9a333f13-5ccc-4109-bce3-0ad629843edf", "client"), //aandrus
                Arguments.of("f14ac5b1-16f3-42ff-8df3-dd95de69c368", "admin") //kwotyla
        );
    }

    private static Stream<Arguments> provideOldUserLevelForAccountParameters() {
        return Stream.of(
                Arguments.of("9a333f13-5ccc-4109-bce3-0ad629843edf", "admin"), //aandrus
                Arguments.of("f512c0b6-40b2-4bcb-8541-46077ac02101", "staff"), //tkarol
                Arguments.of("f14ac5b1-16f3-42ff-8df3-dd95de69c368", "client") //kwotyla
        );
    }

    private static Stream<Arguments> provideConflictingUserLevelForAccountParameters() {
        return Stream.of(
                Arguments.of("f512c0b6-40b2-4bcb-8541-46077ac02101", "client"), //tkarol
                Arguments.of("f14ac5b1-16f3-42ff-8df3-dd95de69c368", "staff") //kwotyla
        );
    }

    private String login(String login, String password, String language) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        AuthenticationLoginDTO accountLoginDTO = new AuthenticationLoginDTO(login, password, language);

        RequestSpecification request = RestAssured.given();
        request.contentType(CONTENT_TYPE);
        request.body(mapper.writeValueAsString(accountLoginDTO));

        Response response = request.post(BASE_URL + "/auth/login-credentials");
        return response.as(AccessAndRefreshTokensDTO.class).getAccessToken();
    }

    private static Stream<Arguments> provideNoAdminLevelAccountsParameters() {
        return Stream.of(
                Arguments.of("tonyhalik"),          // tonyhalik staff
                Arguments.of("adamn")               // adamn client
        );
    }

    private static Stream<Arguments> provideAllLevelAccountsParameters() {
        return Stream.of(
                Arguments.of("tonyhalik"),          // tonyhalik staff
                Arguments.of("adamn"),              // adamn client
                Arguments.of("jerzybem")            // jerzybem admin
        );
    }

    private static Stream<Arguments> provideAllLevelAccountsParametersAndNotValidIfMatch() {
        return Stream.of(
                Arguments.of("tonyhalik", ""),      // tonyhalik staff
                Arguments.of("tonyhalik", "  "),    // tonyhalik staff
                Arguments.of("adamn", ""),          // adamn client
                Arguments.of("adamn", "  "),        // adamn client
                Arguments.of("jerzybem", ""),       // jerzybem admin
                Arguments.of("jerzybem", "  ")      // jerzybem admin
        );
    }
        private static Stream<Arguments> provideNotValidIfMatch () {
            return Stream.of(
                    Arguments.of(""),      // tonyhalik staff
                    Arguments.of(""),    // tonyhalik staff
                    Arguments.of(""),          // adamn client
                    Arguments.of(""),        // adamn client
                    Arguments.of(""),       // jerzybem admin
                    Arguments.of("")      // jerzybem admin
            );
        }

        private static Stream<Arguments> provideInvalidUUIDParameters () {
            return Stream.of(
                    Arguments.of("  "),     // blank
                    Arguments.of("db85e820-69a0-469c-bdb2-2fa38ae6e1c0bdb2"),   // too long
                    Arguments.of("db85e820-69a0-469c-bdb2"),   // too short
                    Arguments.of("db85e820-69a0-469c-bdb2-2fa38ae6e1X0")   // too invalid character
            );
        }

        private static AccountModifyDTO toAccountModifyDTO (AccountOutputDTO account){
            return new AccountModifyDTO(
                    account.getLogin(),
                    account.getVersion(),
                    account.getUserLevelsDto(),
                    account.getName(),
                    account.getLastname(),
                    account.getPhoneNumber(),
                    account.isTwoFactorAuth()
            );
        }

        private static SectorModifyDTO toSectorModifyDTO (SectorOutputDTO sector){
            return new SectorModifyDTO(
                    sector.getId(),
                    sector.getParkingId(),
                    sector.getVersion(),
                    sector.getName(),
                    sector.getType(),
                    sector.getMaxPlaces(),
                    sector.getWeight(),
                    sector.getActive()
            );
        }

        private String decodeJwtTokenAndExtractValue (String payload, String key){
            String[] parts = payload.split("\\.");
            for (String part : parts) {
                byte[] dec = Base64.getDecoder().decode(part);
                String str = new String(dec);

                if (str.contains(key)) {
                    // In JWT token key and value pair comes in "key":"value",
                    // so the first letter of value is equal to the length of key plus 3 characters.
                    str = str.substring(str.indexOf(key) + key.length() + 3);
                    return str.substring(0, str.indexOf("\","));
                }
            }

            return null;
        }
    }
