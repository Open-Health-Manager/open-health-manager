import React from 'react';
import { Switch } from 'react-router-dom';

import ErrorBoundaryRoute from 'app/shared/error/error-boundary-route';

import FHIRPatientConsent from './fhir-patient-consent';
import FHIRPatientConsentDetail from './fhir-patient-consent-detail';

const Routes = ({ match }) => (
  <>
    <Switch>
      <ErrorBoundaryRoute exact path={`${match.url}/:id`} component={FHIRPatientConsentDetail} />
      <ErrorBoundaryRoute path={match.url} component={FHIRPatientConsent} />
    </Switch>
  </>
);

export default Routes;
