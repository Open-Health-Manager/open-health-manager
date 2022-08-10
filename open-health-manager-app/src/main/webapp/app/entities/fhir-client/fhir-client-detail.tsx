import React, { useEffect } from 'react';
import { Link, RouteComponentProps } from 'react-router-dom';
import { Button, Row, Col } from 'reactstrap';
import { Translate } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { APP_DATE_FORMAT, APP_LOCAL_DATE_FORMAT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntity } from './fhir-client.reducer';

export const FHIRClientDetail = (props: RouteComponentProps<{ id: string }>) => {
  const dispatch = useAppDispatch();

  useEffect(() => {
    dispatch(getEntity(props.match.params.id));
  }, []);

  const fHIRClientEntity = useAppSelector(state => state.fHIRClient.entity);
  return (
    <Row>
      <Col md="8">
        <h2 data-cy="fHIRClientDetailsHeading">
          <Translate contentKey="openHealthManagerApp.fHIRClient.detail.title">FHIRClient</Translate>
        </h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">
              <Translate contentKey="global.field.id">ID</Translate>
            </span>
          </dt>
          <dd>{fHIRClientEntity.id}</dd>
          <dt>
            <span id="name">
              <Translate contentKey="openHealthManagerApp.fHIRClient.name">Name</Translate>
            </span>
          </dt>
          <dd>{fHIRClientEntity.name}</dd>
          <dt>
            <span id="displayName">
              <Translate contentKey="openHealthManagerApp.fHIRClient.displayName">Display Name</Translate>
            </span>
          </dt>
          <dd>{fHIRClientEntity.displayName}</dd>
          <dt>
            <span id="uri">
              <Translate contentKey="openHealthManagerApp.fHIRClient.uri">Uri</Translate>
            </span>
          </dt>
          <dd>{fHIRClientEntity.uri}</dd>
          <dt>
            <span id="fhirOrganizationId">
              <Translate contentKey="openHealthManagerApp.fHIRClient.fhirOrganizationId">Fhir Organization Id</Translate>
            </span>
          </dt>
          <dd>{fHIRClientEntity.fhirOrganizationId}</dd>
          <dt>
            <span id="clientDirection">
              <Translate contentKey="openHealthManagerApp.fHIRClient.clientDirection">Client Direction</Translate>
            </span>
          </dt>
          <dd>{fHIRClientEntity.clientDirection}</dd>
        </dl>
        <Button tag={Link} to="/fhir-client" replace color="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
        &nbsp;
        <Button tag={Link} to={`/fhir-client/${fHIRClientEntity.id}/edit`} replace color="primary">
          <FontAwesomeIcon icon="pencil-alt" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.edit">Edit</Translate>
          </span>
        </Button>
      </Col>
    </Row>
  );
};

export default FHIRClientDetail;
