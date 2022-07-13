import React from 'react';
import { Switch } from 'react-router-dom';

import ErrorBoundaryRoute from 'app/shared/error/error-boundary-route';

import FHIRPatient from './fhir-patient';
import FHIRPatientDetail from './fhir-patient-detail';
import FHIRPatientUpdate from './fhir-patient-update';
import FHIRPatientDeleteDialog from './fhir-patient-delete-dialog';

const Routes = ({ match }) => (
  <>
    <Switch>
      <ErrorBoundaryRoute exact path={`${match.url}/new`} component={FHIRPatientUpdate} />
      <ErrorBoundaryRoute exact path={`${match.url}/:id/edit`} component={FHIRPatientUpdate} />
      <ErrorBoundaryRoute exact path={`${match.url}/:id`} component={FHIRPatientDetail} />
      <ErrorBoundaryRoute path={match.url} component={FHIRPatient} />
    </Switch>
    <ErrorBoundaryRoute exact path={`${match.url}/:id/delete`} component={FHIRPatientDeleteDialog} />
  </>
);

export default Routes;
