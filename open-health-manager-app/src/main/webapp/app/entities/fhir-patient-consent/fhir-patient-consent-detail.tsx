import React, { useEffect } from 'react';
import { Link, RouteComponentProps } from 'react-router-dom';
import { Button, Row, Col } from 'reactstrap';
import { Translate, byteSize } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { APP_DATE_FORMAT, APP_LOCAL_DATE_FORMAT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntity } from './fhir-patient-consent.reducer';

export const FHIRPatientConsentDetail = (props: RouteComponentProps<{ id: string }>) => {
  const dispatch = useAppDispatch();

  useEffect(() => {
    dispatch(getEntity(props.match.params.id));
  }, []);

  const fHIRPatientConsentEntity = useAppSelector(state => state.fHIRPatientConsent.entity);
  return (
    <Row>
      <Col md="8">
        <h2 data-cy="fHIRPatientConsentDetailsHeading">
          <Translate contentKey="openHealthManagerApp.fHIRPatientConsent.detail.title">FHIRPatientConsent</Translate>
        </h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">
              <Translate contentKey="global.field.id">ID</Translate>
            </span>
          </dt>
          <dd>{fHIRPatientConsentEntity.id}</dd>
          <dt>
            <span id="fhirResource">
              <Translate contentKey="openHealthManagerApp.fHIRPatientConsent.fhirResource">Fhir Resource</Translate>
            </span>
          </dt>
          <dd>{fHIRPatientConsentEntity.fhirResource}</dd>
          <dt>
            <Translate contentKey="openHealthManagerApp.fHIRPatientConsent.user">User</Translate>
          </dt>
          <dd>{fHIRPatientConsentEntity.user ? fHIRPatientConsentEntity.user.login : ''}</dd>
          <dt>
            <Translate contentKey="openHealthManagerApp.fHIRPatientConsent.client">Client</Translate>
          </dt>
          <dd>{fHIRPatientConsentEntity.client ? fHIRPatientConsentEntity.client.name : ''}</dd>
        </dl>
        <Button tag={Link} to="/fhir-patient-consent" replace color="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
        &nbsp;
        <Button tag={Link} to={`/fhir-patient-consent/${fHIRPatientConsentEntity.id}/edit`} replace color="primary">
          <FontAwesomeIcon icon="pencil-alt" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.edit">Edit</Translate>
          </span>
        </Button>
      </Col>
    </Row>
  );
};

export default FHIRPatientConsentDetail;
