package org.mitre.healthmanager.service.dto;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.mitre.healthmanager.service.LoginMatchEmailException;

public class EmailLoginConstraintValidator implements ConstraintValidator<ValidEmailLogin, AdminUserDTO> {
    @Override
    public void initialize(ValidEmailLogin arg0) {
    }

    @Override
    public boolean isValid(AdminUserDTO userDTO, ConstraintValidatorContext context) {
        if (!userDTO.getLogin().equalsIgnoreCase("admin") && !userDTO.getEmail().equalsIgnoreCase(userDTO.getLogin())) {
        	throw new LoginMatchEmailException();        
        }
        
        return true;
    }
}