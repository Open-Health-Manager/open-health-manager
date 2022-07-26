package org.mitre.healthmanager;

import java.util.Arrays;
import com.google.common.base.Joiner;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.passay.PasswordValidator;
import org.passay.PasswordData;

import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.EnglishSequenceData;
import org.passay.LengthRule;
import org.passay.RuleResult;
import org.passay.WhitespaceRule;
import org.passay.IllegalSequenceRule;



public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public void initialize(ValidPassword arg0) {
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {

        if (password == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Password must not be null.")
              .addConstraintViolation();
            return false;
        }
        PasswordValidator validator = new PasswordValidator(Arrays.asList(
            // length between 3 and 100 characters
            new LengthRule(3, 60),
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
            new IllegalSequenceRule(EnglishSequenceData.Numerical, 5, false)
        ));

        RuleResult result = validator.validate(new PasswordData(password));
        if (result.isValid()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
          Joiner.on(",").join(validator.getMessages(result)))
          .addConstraintViolation();
        return false;
    }
}
