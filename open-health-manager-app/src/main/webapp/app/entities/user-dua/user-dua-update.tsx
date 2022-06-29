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
import { IUserDUA } from 'app/shared/model/user-dua.model';
import { getEntity, updateEntity, createEntity, reset } from './user-dua.reducer';

export const UserDUAUpdate = (props: RouteComponentProps<{ id: string }>) => {
  const dispatch = useAppDispatch();

  const [isNew] = useState(!props.match.params || !props.match.params.id);

  const users = useAppSelector(state => state.userManagement.users);
  const userDUAEntity = useAppSelector(state => state.userDUA.entity);
  const loading = useAppSelector(state => state.userDUA.loading);
  const updating = useAppSelector(state => state.userDUA.updating);
  const updateSuccess = useAppSelector(state => state.userDUA.updateSuccess);
  const handleClose = () => {
    props.history.push('/user-dua' + props.location.search);
  };

  useEffect(() => {
    if (isNew) {
      dispatch(reset());
    } else {
      dispatch(getEntity(props.match.params.id));
    }

    dispatch(getUsers({}));
  }, []);

  useEffect(() => {
    if (updateSuccess) {
      handleClose();
    }
  }, [updateSuccess]);

  const saveEntity = values => {
    values.activeDate = convertDateTimeToServer(values.activeDate);
    values.revocationDate = convertDateTimeToServer(values.revocationDate);

    const entity = {
      ...userDUAEntity,
      ...values,
      user: users.find(it => it.id.toString() === values.user.toString()),
    };

    if (isNew) {
      dispatch(createEntity(entity));
    } else {
      dispatch(updateEntity(entity));
    }
  };

  const defaultValues = () =>
    isNew
      ? {
          activeDate: displayDefaultDateTime(),
          revocationDate: displayDefaultDateTime(),
        }
      : {
          ...userDUAEntity,
          activeDate: convertDateTimeFromServer(userDUAEntity.activeDate),
          revocationDate: convertDateTimeFromServer(userDUAEntity.revocationDate),
          user: userDUAEntity?.user?.id,
        };

  return (
    <div>
      <Row className="justify-content-center">
        <Col md="8">
          <h2 id="openHealthManagerApp.userDUA.home.createOrEditLabel" data-cy="UserDUACreateUpdateHeading">
            <Translate contentKey="openHealthManagerApp.userDUA.home.createOrEditLabel">Create or edit a UserDUA</Translate>
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
                  id="user-dua-id"
                  label={translate('global.field.id')}
                  validate={{ required: true }}
                />
              ) : null}
              <ValidatedField
                label={translate('openHealthManagerApp.userDUA.active')}
                id="user-dua-active"
                name="active"
                data-cy="active"
                check
                type="checkbox"
              />
              <ValidatedField
                label={translate('openHealthManagerApp.userDUA.version')}
                id="user-dua-version"
                name="version"
                data-cy="version"
                type="text"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                }}
              />
              <ValidatedField
                label={translate('openHealthManagerApp.userDUA.ageAttested')}
                id="user-dua-ageAttested"
                name="ageAttested"
                data-cy="ageAttested"
                check
                type="checkbox"
              />
              <ValidatedField
                label={translate('openHealthManagerApp.userDUA.activeDate')}
                id="user-dua-activeDate"
                name="activeDate"
                data-cy="activeDate"
                type="datetime-local"
                placeholder="YYYY-MM-DD HH:mm"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                }}
              />
              <ValidatedField
                label={translate('openHealthManagerApp.userDUA.revocationDate')}
                id="user-dua-revocationDate"
                name="revocationDate"
                data-cy="revocationDate"
                type="datetime-local"
                placeholder="YYYY-MM-DD HH:mm"
              />
              <ValidatedField
                id="user-dua-user"
                name="user"
                data-cy="user"
                label={translate('openHealthManagerApp.userDUA.user')}
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
              <Button tag={Link} id="cancel-save" data-cy="entityCreateCancelButton" to="/user-dua" replace color="info">
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

export default UserDUAUpdate;
