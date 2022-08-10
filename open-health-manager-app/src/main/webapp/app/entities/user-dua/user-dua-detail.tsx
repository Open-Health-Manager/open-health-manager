import React, { useEffect } from 'react';
import { Link, RouteComponentProps } from 'react-router-dom';
import { Button, Row, Col } from 'reactstrap';
import { Translate, TextFormat } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { APP_DATE_FORMAT, APP_LOCAL_DATE_FORMAT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntity } from './user-dua.reducer';

export const UserDUADetail = (props: RouteComponentProps<{ id: string }>) => {
  const dispatch = useAppDispatch();

  useEffect(() => {
    dispatch(getEntity(props.match.params.id));
  }, []);

  const userDUAEntity = useAppSelector(state => state.userDUA.entity);
  return (
    <Row>
      <Col md="8">
        <h2 data-cy="userDUADetailsHeading">
          <Translate contentKey="openHealthManagerApp.userDUA.detail.title">UserDUA</Translate>
        </h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">
              <Translate contentKey="global.field.id">ID</Translate>
            </span>
          </dt>
          <dd>{userDUAEntity.id}</dd>
          <dt>
            <span id="active">
              <Translate contentKey="openHealthManagerApp.userDUA.active">Active</Translate>
            </span>
          </dt>
          <dd>{userDUAEntity.active ? 'true' : 'false'}</dd>
          <dt>
            <span id="version">
              <Translate contentKey="openHealthManagerApp.userDUA.version">Version</Translate>
            </span>
          </dt>
          <dd>{userDUAEntity.version}</dd>
          <dt>
            <span id="ageAttested">
              <Translate contentKey="openHealthManagerApp.userDUA.ageAttested">Age Attested</Translate>
            </span>
          </dt>
          <dd>{userDUAEntity.ageAttested ? 'true' : 'false'}</dd>
          <dt>
            <span id="activeDate">
              <Translate contentKey="openHealthManagerApp.userDUA.activeDate">Active Date</Translate>
            </span>
          </dt>
          <dd>{userDUAEntity.activeDate ? <TextFormat value={userDUAEntity.activeDate} type="date" format={APP_DATE_FORMAT} /> : null}</dd>
          <dt>
            <span id="revocationDate">
              <Translate contentKey="openHealthManagerApp.userDUA.revocationDate">Revocation Date</Translate>
            </span>
          </dt>
          <dd>
            {userDUAEntity.revocationDate ? <TextFormat value={userDUAEntity.revocationDate} type="date" format={APP_DATE_FORMAT} /> : null}
          </dd>
          <dt>
            <Translate contentKey="openHealthManagerApp.userDUA.user">User</Translate>
          </dt>
          <dd>{userDUAEntity.user ? userDUAEntity.user.login : ''}</dd>
        </dl>
        <Button tag={Link} to="/user-dua" replace color="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
        &nbsp;
        <Button tag={Link} to={`/user-dua/${userDUAEntity.id}/edit`} replace color="primary">
          <FontAwesomeIcon icon="pencil-alt" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.edit">Edit</Translate>
          </span>
        </Button>
      </Col>
    </Row>
  );
};

export default UserDUADetail;
