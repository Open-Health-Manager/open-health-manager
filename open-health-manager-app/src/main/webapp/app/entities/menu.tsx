import React from 'react';
import { Translate } from 'react-jhipster';

import MenuItem from 'app/shared/layout/menus/menu-item';

const EntitiesMenu = () => {
  return (
    <>
      {/* prettier-ignore */}

      <MenuItem icon="asterisk" to="/fhir-client">
        <Translate contentKey="global.menu.entities.fhirClient" />
      </MenuItem>
      <MenuItem icon="asterisk" to="/fhir-patient-consent">
        <Translate contentKey="global.menu.entities.fhirPatientConsent" />
      </MenuItem>
      {/* jhipster-needle-add-entity-to-menu - JHipster will add entities to the menu here */}
    </>
  );
};

export default EntitiesMenu as React.ComponentType<any>;
