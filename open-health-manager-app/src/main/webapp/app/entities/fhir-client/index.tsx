import React from 'react';
import { Switch } from 'react-router-dom';

import ErrorBoundaryRoute from 'app/shared/error/error-boundary-route';

import FHIRClient from './fhir-client';
import FHIRClientDetail from './fhir-client-detail';
import FHIRClientUpdate from './fhir-client-update';
import FHIRClientDeleteDialog from './fhir-client-delete-dialog';

const Routes = ({ match }) => (
  <>
    <Switch>
      <ErrorBoundaryRoute exact path={`${match.url}/new`} component={FHIRClientUpdate} />
      <ErrorBoundaryRoute exact path={`${match.url}/:id/edit`} component={FHIRClientUpdate} />
      <ErrorBoundaryRoute exact path={`${match.url}/:id`} component={FHIRClientDetail} />
      <ErrorBoundaryRoute path={match.url} component={FHIRClient} />
    </Switch>
    <ErrorBoundaryRoute exact path={`${match.url}/:id/delete`} component={FHIRClientDeleteDialog} />
  </>
);

export default Routes;
