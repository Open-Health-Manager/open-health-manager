import React, { useState, useEffect } from 'react';
import { Link, RouteComponentProps } from 'react-router-dom';
import { Button, Row, Col, FormText } from 'reactstrap';
import { isNumber, Translate, translate, ValidatedField, ValidatedForm } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { convertDateTimeFromServer, convertDateTimeToServer, displayDefaultDateTime } from 'app/shared/util/date-utils';
import { mapIdList } from 'app/shared/util/entity-utils';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { IFHIRClient } from 'app/shared/model/fhir-client.model';
import { ClientDirection } from 'app/shared/model/enumerations/client-direction.model';
import { getEntity, updateEntity, createEntity, reset } from './fhir-client.reducer';

export const FHIRClientUpdate = (props: RouteComponentProps<{ id: string }>) => {
  const dispatch = useAppDispatch();

  const [isNew] = useState(!props.match.params || !props.match.params.id);

  const fHIRClientEntity = useAppSelector(state => state.fHIRClient.entity);
  const loading = useAppSelector(state => state.fHIRClient.loading);
  const updating = useAppSelector(state => state.fHIRClient.updating);
  const updateSuccess = useAppSelector(state => state.fHIRClient.updateSuccess);
  const clientDirectionValues = Object.keys(ClientDirection);
  const handleClose = () => {
    props.history.push('/fhir-client' + props.location.search);
  };

  useEffect(() => {
    if (isNew) {
      dispatch(reset());
    } else {
      dispatch(getEntity(props.match.params.id));
    }
  }, []);

  useEffect(() => {
    if (updateSuccess) {
      handleClose();
    }
  }, [updateSuccess]);

  const saveEntity = values => {
    const entity = {
      ...fHIRClientEntity,
      ...values,
    };

    if (isNew) {
      dispatch(createEntity(entity));
    } else {
      dispatch(updateEntity(entity));
    }
  };

  const defaultValues = () =>
    isNew
      ? {}
      : {
          clientDirection: 'OUTBOUND',
          ...fHIRClientEntity,
        };

  return (
    <div>
      <Row className="justify-content-center">
        <Col md="8">
          <h2 id="openHealthManagerApp.fHIRClient.home.createOrEditLabel" data-cy="FHIRClientCreateUpdateHeading">
            <Translate contentKey="openHealthManagerApp.fHIRClient.home.createOrEditLabel">Create or edit a FHIRClient</Translate>
          </h2>
        </Col>
      </Row>
      <Row className="justify-content-center">
        <Col md="8">
          {loading ? (
            <p>Loading...</p>
          ) : (
            <ValidatedForm defaultValues={defaultValues()} onSubmit={saveEntity}>
              {!isNew ? (
                <ValidatedField
                  name="id"
                  required
                  readOnly
                  id="fhir-client-id"
                  label={translate('global.field.id')}
                  validate={{ required: true }}
                />
              ) : null}
              <ValidatedField
                label={translate('openHealthManagerApp.fHIRClient.name')}
                id="fhir-client-name"
                name="name"
                data-cy="name"
                type="text"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                }}
              />
              <ValidatedField
                label={translate('openHealthManagerApp.fHIRClient.displayName')}
                id="fhir-client-displayName"
                name="displayName"
                data-cy="displayName"
                type="text"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                }}
              />
              <ValidatedField
                label={translate('openHealthManagerApp.fHIRClient.uri')}
                id="fhir-client-uri"
                name="uri"
                data-cy="uri"
                type="text"
              />
              <ValidatedField
                label={translate('openHealthManagerApp.fHIRClient.fhirOrganizationId')}
                id="fhir-client-fhirOrganizationId"
                name="fhirOrganizationId"
                data-cy="fhirOrganizationId"
                type="text"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                }}
              />
              <ValidatedField
                label={translate('openHealthManagerApp.fHIRClient.clientDirection')}
                id="fhir-client-clientDirection"
                name="clientDirection"
                data-cy="clientDirection"
                type="select"
              >
                {clientDirectionValues.map(clientDirection => (
                  <option value={clientDirection} key={clientDirection}>
                    {translate('openHealthManagerApp.ClientDirection.' + clientDirection)}
                  </option>
                ))}
              </ValidatedField>
              <Button tag={Link} id="cancel-save" data-cy="entityCreateCancelButton" to="/fhir-client" replace color="info">
                <FontAwesomeIcon icon="arrow-left" />
                &nbsp;
                <span className="d-none d-md-inline">
                  <Translate contentKey="entity.action.back">Back</Translate>
                </span>
              </Button>
              &nbsp;
              <Button color="primary" id="save-entity" data-cy="entityCreateSaveButton" type="submit" disabled={updating}>
                <FontAwesomeIcon icon="save" />
                &nbsp;
                <Translate contentKey="entity.action.save">Save</Translate>
              </Button>
            </ValidatedForm>
          )}
        </Col>
      </Row>
    </div>
  );
};

export default FHIRClientUpdate;
