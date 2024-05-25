package pl.lodz.p.it.ssbd2024.ssbd03.utils;

import java.util.Locale;
import java.util.ResourceBundle;

public class I18n {

    // Account exceptions
    public static final String ACCOUNT_CONSTRAINT_VIOLATION = "account.constraint.violation.exception";
    public static final String ACCOUNT_EMAIL_NOT_FOUND = "account.with.given.email.not.found.exception";
    public static final String ACCOUNT_ID_NOT_FOUND = "account.with.given.id.not.found.exception";
    public static final String ACCOUNT_BLOCKED_EXCEPTION = "account.status.blocked.exception";
    public static final String ACCOUNT_INACTIVE_EXCEPTION = "account.status.inactive.exception";
    public static final String ACCOUNT_AUTHENTICATION_EXCEPTION = "account.authentication.exception";

    // Account reset own password exceptions
    public static final String SET_NEW_PASSWORD_IS_THE_SAME_AS_CURRENT_ONE = "account.reset.password.same.passwords.exception";
    public static final String INCORRECT_PASSWORD = "account.reset.password.incorrect.current.password";
    public static final String PASSWORD_PREVIOUSLY_USED = "account.reset.password.password.previously.used.exception";

    // Account internationalization keys
    public static final String ACCOUNT_LOGIN_ALREADY_TAKEN = "account.login.already.taken.exception";
    public static final String ACCOUNT_EMAIL_ALREADY_TAKEN = "account.email.already.taken.exception";
    public static final String ACCOUNT_BLOCKED = "account.blocked.exception";
    public static final String ACCOUNT_BLOCKED_BY_ADMIN = "account.blocked.by.admin.exception";
    public static final String ACCOUNT_BLOCKED_BY_FAILED_LOGIN_ATTEMPTS = "account.blocked.by.too.many.failed.attempts.exception";
    public static final String ACCOUNT_ALREADY_BLOCKED = "account.already.blocked.exception";
    public static final String ACCOUNT_ALREADY_UNBLOCKED = "account.already.unblocked.exception";

    // Token exceptions
    public static final String TOKEN_VALUE_NOT_FOUND_EXCEPTION = "token.token.value.not.found.exception";
    public static final String TOKEN_NOT_VALID_EXCEPTION = "token.token.value.not.valid.exception";
    public static final String TOKEN_DATA_EXTRACTION_EXCEPTION = "token.data.extraction.taken.exception";

    // Token internationalization keys
    public static final String TOKEN_VALUE_ALREADY_TAKEN = "token.value.already.taken.exception";

    // General exception messages
    public static final String OPTIMISTIC_LOCK_EXCEPTION = "application.optimistic.lock.exception";
    public static final String INTERNAL_SERVER_ERROR = "application.internal.server.error.exception";
    public static final String UNAUTHORIZED_EXCEPTION = "application.unauthorized.exception";
    public static final String ACCESS_DENIED_EXCEPTION = "application.access.denied.exception";
    public static final String UNEXPECTED_DATABASE_EXCEPTION = "application.database.server.exception";
    public static final String PATH_NOT_FOUND_EXCEPTION = "application.path.not.found.exception";
    public static final String UNSUPPORTED_OPERATION_EXCEPTION = "application.unsupported.operation.exception";

    // Account service
    public static final String ADMIN_ACCOUNT_REMOVE_OWN_ADMIN_USER_LEVEL_EXCEPTION = "account.service.admin.remove.own.admin.user.level.exception";
    public static final String NO_SUCH_USER_LEVEL_EXCEPTION = "user_level.no.such.user.level.exception";
    public static final String ONE_USER_LEVEL = "user_level.one.user.level";
    public static final String UNEXPECTED_CLIENT_TYPE = "user_level.client.client_type.unexpected.exception";
    public static final String USER_LEVEL_DUPLICATED = "user_level.type.duplicated.exception";

    // Account service
    public static final String INVALID_LOGIN_ATTEMPT_EXCEPTION = "account.service.invalid.login.attempt.exception";
    public static final String ACCOUNT_NOT_FOUND_EXCEPTION = "account.service.account.not.found.exception";
    public static final String ACCOUNT_CONSTRAINT_VALIDATION_EXCEPTION = "account.service.account.constraint.validation.exception";
    public static final String ACCOUNT_SAME_EMAIL_EXCEPTION = "account.service.account.same.email.exception";
    public static final String ACCOUNT_EMAIL_COLLISION_EXCEPTION = "account.service.account.email.collision.exception";
    public static final String ACCOUNT_TRY_TO_BLOCK_OWN_EXCEPTION = "account.service.account.try_to_block_own.exception";
    public static final String ACCOUNT_EMAIL_FROM_TOKEN_NULL_EXCEPTION = "account.service.email_from_token_null.exception";
    public static final String TOKEN_NOT_FOUND_EXCEPTION = "token.not.found.exception";

    // Account controller
    public static final String TOKEN_INVALID_OR_EXPIRED = "account.controller.token.invalid.or.expired";
    public static final String UUID_INVALID = "account.controller.uuid.invalid";
    public static final String BAD_UUID_INVALID_FORMAT_EXCEPTION = "account.controller.uuid.invalid.format.exception";
    public static final String MISSING_HEADER_IF_MATCH = "account.controller.missing.header.if_match.exception";

    // JWT
    public static final String DATA_INTEGRITY_COMPROMISED = "controller.data.integrity.compromised.exception";

    // Util
    public static final String USER_LEVEL_MISSING = "controller.data.integrity.user_level.missing.exception";

    // Mail provider
    public static final String CONFIRM_REGISTER_GREETING_MESSAGE = "mail.confirm.register.greeting.message";
    public static final String CONFIRM_REGISTER_MESSAGE_SUBJECT = "mail.confirm.register.message.subject";
    public static final String CONFIRM_REGISTER_RESULT_MESSAGE = "mail.confirm.register.result_message";
    public static final String CONFIRM_REGISTER_ACTION_DESCRIPTION = "mail.confirm.register.action_description";
    public static final String CONFIRM_REGISTER_NOTE_TITLE = "mail.confirm.register.note_title";

    public static final String LOGIN_AUTHENTICATION_CODE_GREETING_MESSAGE = "mail.login.auth.code.greeting.message";
    public static final String LOGIN_AUTHENTICATION_CODE_MESSAGE_SUBJECT = "mail.login.auth.code.message.subject";
    public static final String LOGIN_AUTHENTICATION_CODE_RESULT_MESSAGE = "mail.login.auth.code.result_message";
    public static final String LOGIN_AUTHENTICATION_CODE_ACTION_DESCRIPTION = "mail.login.auth.code.action_description";
    public static final String LOGIN_AUTHENTICATION_CODE_NOTE_TITLE = "mail.login.auth.code.note_title";

    public static final String CONFIRM_ACCOUNT_ACTIVATION_GREETING_MESSAGE = "mail.confirm.account.activation.greeting.message";
    public static final String CONFIRM_ACCOUNT_ACTIVATION_MESSAGE_SUBJECT = "mail.confirm.account.activation.message.subject";
    public static final String CONFIRM_ACCOUNT_ACTIVATION_RESULT_MESSAGE = "mail.confirm.account.activation.result_message";
    public static final String CONFIRM_ACCOUNT_ACTIVATION_ACTION_DESCRIPTION = "mail.confirm.account.activation.action_description";
    public static final String CONFIRM_ACCOUNT_ACTIVATION_NOTE_TITLE = "mail.confirm.account.activation.note_title";

    public static final String CONFIRM_EMAIL_GREETING_MESSAGE = "mail.confirm.email.greeting.message";
    public static final String CONFIRM_EMAIL_MESSAGE_SUBJECT = "mail.confirm.email.message.subject";
    public static final String CONFIRM_EMAIL_RESULT_MESSAGE = "mail.confirm.email.result_message";
    public static final String CONFIRM_EMAIL_ACTION_DESCRIPTION = "mail.confirm.email.action_description";
    public static final String CONFIRM_EMAIL_NOTE_TITLE = "mail.confirm.email.note_title";

    public static final String BLOCK_ACCOUNT_GREETING_MESSAGE = "mail.block.account.greeting.message";
    public static final String BLOCK_ACCOUNT_MESSAGE_SUBJECT = "mail.block.account.message.subject";
    public static final String BLOCK_ACCOUNT_RESULT_MESSAGE_AUTO = "mail.block.account.result_message_auto";
    public static final String BLOCK_ACCOUNT_RESULT_MESSAGE_ADMIN = "mail.block.account.result_message_admin";
    public static final String BLOCK_ACCOUNT_ACTION_DESCRIPTION_AUTO = "mail.block.account.action_description_auto";
    public static final String BLOCK_ACCOUNT_ACTION_DESCRIPTION_ADMIN = "mail.block.account.action_description_admin";
    public static final String BLOCK_ACCOUNT_NOTE_TITLE = "mail.block.account.note_title";

    public static final String UNBLOCK_ACCOUNT_GREETING_MESSAGE = "mail.unblock.account.greeting.message";
    public static final String UNBLOCK_ACCOUNT_MESSAGE_SUBJECT = "mail.unblock.account.message.subject";
    public static final String UNBLOCK_ACCOUNT_RESULT_MESSAGE = "mail.unblock.account.result_message";
    public static final String UNBLOCK_ACCOUNT_ACTION_DESCRIPTION = "mail.unblock.account.action_description";
    public static final String UNBLOCK_ACCOUNT_NOTE_TITLE = "mail.unblock.account.note_title";

    public static final String ACCESS_LEVEL_GRANTED_GREETING_MESSAGE = "mail.granted.user.level.greeting.message";
    public static final String ACCESS_LEVEL_GRANTED_MESSAGE_SUBJECT = "mail.granted.user.level.message.subject";
    public static final String ACCESS_LEVEL_GRANTED_RESULT_MESSAGE = "mail.granted.user.level.result_message";
    public static final String ACCESS_LEVEL_GRANTED_ACTION_DESCRIPTION = "mail.granted.user.level.action_description";
    public static final String ACCESS_LEVEL_GRANTED_NOTE_TITLE = "mail.granted.user.level.note_title";

    public static final String ACCESS_LEVEL_REVOKED_GREETING_MESSAGE = "mail.revoked.user.level.greeting.message";
    public static final String ACCESS_LEVEL_REVOKED_MESSAGE_SUBJECT = "mail.revoked.user.level.message.subject";
    public static final String ACCESS_LEVEL_REVOKED_RESULT_MESSAGE = "mail.revoked.user.level.result_message";
    public static final String ACCESS_LEVEL_REVOKED_ACTION_DESCRIPTION = "mail.revoked.user.level.action_description";
    public static final String ACCESS_LEVEL_REVOKED_NOTE_TITLE = "mail.revoked.user.level.note_title";

    public static final String PASSWORD_RESET_GREETING_MESSAGE = "mail.reset.password.greeting.message";
    public static final String PASSWORD_RESET_MESSAGE_SUBJECT = "mail.reset.password.message.subject";
    public static final String PASSWORD_RESET_RESULT_MESSAGE = "mail.reset.password.result_message";
    public static final String PASSWORD_RESET_ACTION_DESCRIPTION = "mail.reset.password.action_description";
    public static final String PASSWORD_RESET_NOTE_TITLE = "mail.reset.password.note_title";

    public static final String AUTO_GENERATED_MESSAGE_NOTE = "mail.auto.generate.message.note";

    public static final String CLIENT_USER_LEVEL = "user.level.client";
    public static final String STAFF_USER_LEVEL = "user.level.staff";
    public static final String ADMIN_USER_LEVEL = "user.level.admin";

    public static String getMessage(String messageKey, String language) {
        Locale locale = new Locale.Builder().setLanguage(language).build();
        ResourceBundle resourceBundle = ResourceBundle.getBundle("Messages", locale);
        return resourceBundle.getString(messageKey);
    }
}
