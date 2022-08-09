import React from 'react';
import { Switch } from 'react-router-dom';
import ErrorBoundaryRoute from 'app/shared/error/error-boundary-route';

import FHIRPatient from './fhir-patient';
import UserDUA from './user-dua';
import FHIRClient from './fhir-client';
import FHIRPatientConsent from './fhir-patient-consent';
/* jhipster-needle-add-route-import - JHipster will add routes here */

export default ({ match }) => {
  return (
    <div>
      <Switch>
        {/* prettier-ignore */}
        <ErrorBoundaryRoute path={`${match.url}fhir-patient`} component={FHIRPatient} />
        <ErrorBoundaryRoute path={`${match.url}user-dua`} component={UserDUA} />
        <ErrorBoundaryRoute path={`${match.url}fhir-client`} component={FHIRClient} />
        <ErrorBoundaryRoute path={`${match.url}fhir-patient-consent`} component={FHIRPatientConsent} />
        {/* jhipster-needle-add-route-path - JHipster will add routes here */}
      </Switch>
    </div>
  );
};
