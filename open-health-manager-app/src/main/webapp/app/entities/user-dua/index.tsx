import React from 'react';
import { Switch } from 'react-router-dom';

import ErrorBoundaryRoute from 'app/shared/error/error-boundary-route';

import UserDUA from './user-dua';
import UserDUADetail from './user-dua-detail';
import UserDUAUpdate from './user-dua-update';
import UserDUADeleteDialog from './user-dua-delete-dialog';

const Routes = ({ match }) => (
  <>
    <Switch>
      <ErrorBoundaryRoute exact path={`${match.url}/new`} component={UserDUAUpdate} />
      <ErrorBoundaryRoute exact path={`${match.url}/:id/edit`} component={UserDUAUpdate} />
      <ErrorBoundaryRoute exact path={`${match.url}/:id`} component={UserDUADetail} />
      <ErrorBoundaryRoute path={match.url} component={UserDUA} />
    </Switch>
    <ErrorBoundaryRoute exact path={`${match.url}/:id/delete`} component={UserDUADeleteDialog} />
  </>
);

export default Routes;
