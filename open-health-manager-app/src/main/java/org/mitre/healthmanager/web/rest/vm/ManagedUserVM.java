package org.mitre.healthmanager.web.rest.vm;

import org.mitre.healthmanager.service.dto.AdminUserDTO;
import org.mitre.healthmanager.service.dto.ValidPasswordClass;

/**
 * View Model extending the AdminUserDTO, which is meant to be used in the user management UI.
 */
@ValidPasswordClass
public class ManagedUserVM extends AdminUserDTO {

    private String password;

    public ManagedUserVM() {
        // Empty constructor needed for Jackson.
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ManagedUserVM{" + super.toString() + "} ";
    }
}
