package org.mitre.healthmanager.web.rest.vm;

import org.mitre.healthmanager.service.dto.UserDUADTO;

/**
 * View Model extending the ManagedUserVM class with a UserDUADTO object included
 */
public class DUAManagedUserVM extends ManagedUserVM {

    private UserDUADTO userDUADTO;

    public DUAManagedUserVM() {
        // Empty constructor needed for Jackson.
    }

    public DUAManagedUserVM(UserDUADTO userDUADTO) {
        this.userDUADTO = userDUADTO;
    }

    public UserDUADTO getUserDUADTO() {
        return userDUADTO;
    }

    public void setUserDUADTO(UserDUADTO userDUADTO) {
        this.userDUADTO = userDUADTO;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DUAManagedUserVM{" + super.toString() + "} ";
    }
}
