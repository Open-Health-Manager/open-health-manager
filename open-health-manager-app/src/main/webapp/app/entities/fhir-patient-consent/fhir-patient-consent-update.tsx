import React, { useState, useEffect } from 'react';
import { Link, RouteComponentProps } from 'react-router-dom';
import { Button, Row, Col, FormText } from 'reactstrap';
import { isNumber, Translate, translate, ValidatedField, ValidatedForm } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { convertDateTimeFromServer, convertDateTimeToServer, displayDefaultDateTime } from 'app/shared/util/date-utils';
import { mapIdList } from 'app/shared/util/entity-utils';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { IUser } from 'app/shared/model/user.model';
import { getUsers } from 'app/modules/administration/user-management/user-management.reducer';
import { IFHIRClient } from 'app/shared/model/fhir-client.model';
import { getEntities as getFHirClients } from 'app/entities/fhir-client/fhir-client.reducer';
import { IFHIRPatientConsent } from 'app/shared/model/fhir-patient-consent.model';
import { getEntity, updateEntity, createEntity, reset } from './fhir-patient-consent.reducer';

export const FHIRPatientConsentUpdate = (props: RouteComponentProps<{ id: string }>) => {
  const dispatch = useAppDispatch();

  const [isNew] = useState(!props.match.params || !props.match.params.id);

  const users = useAppSelector(state => state.userManagement.users);
  const fHIRClients = useAppSelector(state => state.fHIRClient.entities);
  const fHIRPatientConsentEntity = useAppSelector(state => state.fHIRPatientConsent.entity);
  const loading = useAppSelector(state => state.fHIRPatientConsent.loading);
  const updating = useAppSelector(state => state.fHIRPatientConsent.updating);
  const updateSuccess = useAppSelector(state => state.fHIRPatientConsent.updateSuccess);
  const handleClose = () => {
    props.history.push('/fhir-patient-consent' + props.location.search);
  };

  useEffect(() => {
    if (isNew) {
      dispatch(reset());
    } else {
      dispatch(getEntity(props.match.params.id));
    }

    dispatch(getUsers({}));
    dispatch(getFHirClients({}));
  }, []);

  useEffect(() => {
    if (updateSuccess) {
      handleClose();
    }
  }, [updateSuccess]);

  const saveEntity = values => {
    const entity = {
      ...fHIRPatientConsentEntity,
      ...values,
      user: users.find(it => it.id.toString() === values.user.toString()),
      client: fHIRClients.find(it => it.id.toString() === values.client.toString()),
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
          ...fHIRPatientConsentEntity,
          user: fHIRPatientConsentEntity?.user?.id,
          client: fHIRPatientConsentEntity?.client?.id,
        };

  return (
    <div>
      <Row className="justify-content-center">
        <Col md="8">
          <h2 id="openHealthManagerApp.fHIRPatientConsent.home.createOrEditLabel" data-cy="FHIRPatientConsentCreateUpdateHeading">
            <Translate contentKey="openHealthManagerApp.fHIRPatientConsent.home.createOrEditLabel">
              Create or edit a FHIRPatientConsent
            </Translate>
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
                  id="fhir-patient-consent-id"
                  label={translate('global.field.id')}
                  validate={{ required: true }}
                />
              ) : null}
              <ValidatedField
                label={translate('openHealthManagerApp.fHIRPatientConsent.approve')}
                id="fhir-patient-consent-approve"
                name="approve"
                data-cy="approve"
                check
                type="checkbox"
              />
              <ValidatedField
                label={translate('openHealthManagerApp.fHIRPatientConsent.fhirResource')}
                id="fhir-patient-consent-fhirResource"
                name="fhirResource"
                data-cy="fhirResource"
                type="textarea"
              />
              <ValidatedField
                id="fhir-patient-consent-user"
                name="user"
                data-cy="user"
                label={translate('openHealthManagerApp.fHIRPatientConsent.user')}
                type="select"
                required
              >
                <option value="" key="0" />
                {users
                  ? users.map(otherEntity => (
                      <option value={otherEntity.id} key={otherEntity.id}>
                        {otherEntity.login}
                      </option>
                    ))
                  : null}
              </ValidatedField>
              <FormText>
                <Translate contentKey="entity.validation.required">This field is required.</Translate>
              </FormText>
              <ValidatedField
                id="fhir-patient-consent-client"
                name="client"
                data-cy="client"
                label={translate('openHealthManagerApp.fHIRPatientConsent.client')}
                type="select"
              >
                <option value="" key="0" />
                {fHIRClients
                  ? fHIRClients.map(otherEntity => (
                      <option value={otherEntity.id} key={otherEntity.id}>
                        {otherEntity.name}
                      </option>
                    ))
                  : null}
              </ValidatedField>
              <Button tag={Link} id="cancel-save" data-cy="entityCreateCancelButton" to="/fhir-patient-consent" replace color="info">
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

export default FHIRPatientConsentUpdate;
