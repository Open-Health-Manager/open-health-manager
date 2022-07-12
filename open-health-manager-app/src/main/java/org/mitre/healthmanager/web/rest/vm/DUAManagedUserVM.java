package org.mitre.healthmanager.web.rest.vm;

import org.mitre.healthmanager.domain.UserDUA;
import org.mitre.healthmanager.service.dto.AdminUserDTO;

/**
 * View Model extending the ManagedUserVM class with a UserDUA object included
 */
public class DUAManagedUserVM extends ManagedUserVM {

    private UserDUA userDUA;

    public DUAManagedUserVM() {
        // Empty constructor needed for Jackson.
    }

    public DUAManagedUserVM(UserDUA userDUA) {
        this.userDUA = userDUA;
    }

    public UserDUA getUserDUA() {
        return userDUA;
    }

    public void setUserDUA(UserDUA userDUA) {
        this.userDUA = userDUA;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ManagedUserVM{" + super.toString() + "} ";
    }
}
