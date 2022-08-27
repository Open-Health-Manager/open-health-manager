package org.mitre.healthmanager.service.dto;

import java.util.Arrays;
import java.util.Optional;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.passay.PasswordValidator;
import org.passay.PasswordData;
import org.mitre.healthmanager.domain.User;
import org.mitre.healthmanager.repository.UserRepository;

import static org.mitre.healthmanager.security.SecurityUtils.getCurrentUserLogin;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.EnglishSequenceData;
import org.passay.LengthRule;
import org.passay.RuleResult;
import org.passay.UsernameRule;
import org.passay.WhitespaceRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.passay.IllegalSequenceRule;



public class PasswordConstraintClassValidator implements ConstraintValidator<ValidPasswordClass, Object> {
    public static final int PASSWORD_MIN_LENGTH = 6;

    public static final int PASSWORD_MAX_LENGTH = 50;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void initialize(ValidPasswordClass arg0) {
    }

    @Override
    public boolean isValid(Object hasPassword, ConstraintValidatorContext context) {

        
        PasswordValidator validator = new PasswordValidator(Arrays.asList(
            // length between 4 and 100 characters
            new LengthRule(PASSWORD_MIN_LENGTH, PASSWORD_MAX_LENGTH),
            // at least one upper-case character
            new CharacterRule(EnglishCharacterData.UpperCase, 1),
            // at least one lower-case character
            new CharacterRule(EnglishCharacterData.LowerCase, 1),
            // at least one digit character
            new CharacterRule(EnglishCharacterData.Digit, 1),
            // at least one symbol (special character)
            new CharacterRule(EnglishCharacterData.Special, 1),
            // no whitespace
            new WhitespaceRule(),
            // rejects passwords that contain a sequence of >= 5 characters alphabetical  (e.g. abcdef)
            new IllegalSequenceRule(EnglishSequenceData.Alphabetical, 5, false),
            // rejects passwords that contain a sequence of >= 5 characters numerical   (e.g. 12345)
            new IllegalSequenceRule(EnglishSequenceData.Numerical, 5, false),
            // rejects passwords that are the same as the username / login
            new UsernameRule(false, true)
        ));

        String username = getUsername(hasPassword);
        if (username == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Username required for validation.")
                .addPropertyNode("login").addConstraintViolation();
            return false;
        }

        String password = getPassword(hasPassword);
        if (password == null  ) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Password must not be null.")
                .addPropertyNode(getPasswordField(hasPassword)).addConstraintViolation();
            return false;
        }

        RuleResult result = validator.validate(new PasswordData(username, password));
        if (result.isValid()) {
            return true;
        }

        String passwordField = getPasswordField(hasPassword);
        context.disableDefaultConstraintViolation();
        result.getDetails().forEach(rule -> {
            context.buildConstraintViolationWithTemplate(rule.getErrorCode())
            .addPropertyNode(passwordField).addConstraintViolation();
        });
        return false;
    }

    private String getUsername(Object hasPassword) {
        if (hasPassword instanceof PasswordChangeDTO) {
            return getCurrentUserLogin().orElse(null);
        }
        else if (hasPassword instanceof KeyAndPasswordVM) {
            KeyAndPasswordVM passwordReset = (KeyAndPasswordVM) hasPassword;
            String username = null;
            Optional<User> resetUserOption = userRepository.findOneByResetKey(passwordReset.getKey());
            if (resetUserOption.isPresent()) {
                username = resetUserOption.get().getLogin();
            }
            return username;
        }
        else if (hasPassword instanceof ManagedUserVM) {
            ManagedUserVM userData = (ManagedUserVM) hasPassword;
            return userData.getLogin();
        }
        else {
            return null;
        }
    }

    private String getPassword(Object hasPassword) {
        if (hasPassword instanceof PasswordChangeDTO) {
            PasswordChangeDTO passwordChange = (PasswordChangeDTO) hasPassword;
            return passwordChange.getNewPassword();
        }
        else if (hasPassword instanceof KeyAndPasswordVM) {
            KeyAndPasswordVM passwordReset = (KeyAndPasswordVM) hasPassword;
            return passwordReset.getNewPassword();
        }
        else if (hasPassword instanceof ManagedUserVM) {
            ManagedUserVM userData = (ManagedUserVM) hasPassword;
            return userData.getPassword();
        }
        else {
            return null;
        }
    }

    private String getPasswordField(Object hasPassword) {
        if (hasPassword instanceof PasswordChangeDTO) {
            return "newPassword";
        }
        else if (hasPassword instanceof KeyAndPasswordVM) {
            return "newPassword";
        }
        else if (hasPassword instanceof ManagedUserVM) {
            return "password";
        }
        else {
            return null; 
        }
    }
}
