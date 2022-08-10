import React, { useEffect } from 'react';
import { Link, RouteComponentProps } from 'react-router-dom';
import { Button, Row, Col } from 'reactstrap';
import { Translate } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { APP_DATE_FORMAT, APP_LOCAL_DATE_FORMAT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntity } from './fhir-patient.reducer';

export const FHIRPatientDetail = (props: RouteComponentProps<{ id: string }>) => {
  const dispatch = useAppDispatch();

  useEffect(() => {
    dispatch(getEntity(props.match.params.id));
  }, []);

  const fHIRPatientEntity = useAppSelector(state => state.fHIRPatient.entity);
  return (
    <Row>
      <Col md="8">
        <h2 data-cy="fHIRPatientDetailsHeading">
          <Translate contentKey="openHealthManagerApp.fHIRPatient.detail.title">FHIRPatient</Translate>
        </h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">
              <Translate contentKey="global.field.id">ID</Translate>
            </span>
          </dt>
          <dd>{fHIRPatientEntity.id}</dd>
          <dt>
            <span id="fhirId">
              <Translate contentKey="openHealthManagerApp.fHIRPatient.fhirId">Fhir Id</Translate>
            </span>
          </dt>
          <dd>{fHIRPatientEntity.fhirId}</dd>
          <dt>
            <Translate contentKey="openHealthManagerApp.fHIRPatient.user">User</Translate>
          </dt>
          <dd>{fHIRPatientEntity.user ? fHIRPatientEntity.user.login : ''}</dd>
        </dl>
        <Button tag={Link} to="/fhir-patient" replace color="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
        &nbsp;
        <Button tag={Link} to={`/fhir-patient/${fHIRPatientEntity.id}/edit`} replace color="primary">
          <FontAwesomeIcon icon="pencil-alt" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.edit">Edit</Translate>
          </span>
        </Button>
      </Col>
    </Row>
  );
};

export default FHIRPatientDetail;
