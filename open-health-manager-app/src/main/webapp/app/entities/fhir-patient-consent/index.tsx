import React from 'react';
import { Switch } from 'react-router-dom';

import ErrorBoundaryRoute from 'app/shared/error/error-boundary-route';

import FHIRPatientConsent from './fhir-patient-consent';
import FHIRPatientConsentDetail from './fhir-patient-consent-detail';
import FHIRPatientConsentUpdate from './fhir-patient-consent-update';
import FHIRPatientConsentDeleteDialog from './fhir-patient-consent-delete-dialog';

const Routes = ({ match }) => (
  <>
    <Switch>
      <ErrorBoundaryRoute exact path={`${match.url}/new`} component={FHIRPatientConsentUpdate} />
      <ErrorBoundaryRoute exact path={`${match.url}/:id/edit`} component={FHIRPatientConsentUpdate} />
      <ErrorBoundaryRoute exact path={`${match.url}/:id`} component={FHIRPatientConsentDetail} />
      <ErrorBoundaryRoute path={match.url} component={FHIRPatientConsent} />
    </Switch>
    <ErrorBoundaryRoute exact path={`${match.url}/:id/delete`} component={FHIRPatientConsentDeleteDialog} />
  </>
);

export default Routes;
