import React, { useState, useEffect } from 'react';
import { Link, RouteComponentProps } from 'react-router-dom';
import { Button, Table } from 'reactstrap';
import { Translate } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { APP_DATE_FORMAT, APP_LOCAL_DATE_FORMAT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { IFHIRPatient } from 'app/shared/model/fhir-patient.model';
import { getEntities } from './fhir-patient.reducer';

export const FHIRPatient = (props: RouteComponentProps<{ url: string }>) => {
  const dispatch = useAppDispatch();

  const fHIRPatientList = useAppSelector(state => state.fHIRPatient.entities);
  const loading = useAppSelector(state => state.fHIRPatient.loading);

  useEffect(() => {
    dispatch(getEntities({}));
  }, []);

  const handleSyncList = () => {
    dispatch(getEntities({}));
  };

  const { match } = props;

  return (
    <div>
      <h2 id="fhir-patient-heading" data-cy="FHIRPatientHeading">
        <Translate contentKey="openHealthManagerApp.fHIRPatient.home.title">FHIR Patients</Translate>
        <div className="d-flex justify-content-end">
          <Button className="me-2" color="info" onClick={handleSyncList} disabled={loading}>
            <FontAwesomeIcon icon="sync" spin={loading} />{' '}
            <Translate contentKey="openHealthManagerApp.fHIRPatient.home.refreshListLabel">Refresh List</Translate>
          </Button>
          <Link to="/fhir-patient/new" className="btn btn-primary jh-create-entity" id="jh-create-entity" data-cy="entityCreateButton">
            <FontAwesomeIcon icon="plus" />
            &nbsp;
            <Translate contentKey="openHealthManagerApp.fHIRPatient.home.createLabel">Create new FHIR Patient</Translate>
          </Link>
        </div>
      </h2>
      <div className="table-responsive">
        {fHIRPatientList && fHIRPatientList.length > 0 ? (
          <Table responsive>
            <thead>
              <tr>
                <th>
                  <Translate contentKey="openHealthManagerApp.fHIRPatient.id">ID</Translate>
                </th>
                <th>
                  <Translate contentKey="openHealthManagerApp.fHIRPatient.fhirId">Fhir Id</Translate>
                </th>
                <th>
                  <Translate contentKey="openHealthManagerApp.fHIRPatient.user">User</Translate>
                </th>
                <th />
              </tr>
            </thead>
            <tbody>
              {fHIRPatientList.map((fHIRPatient, i) => (
                <tr key={`entity-${i}`} data-cy="entityTable">
                  <td>
                    <Button tag={Link} to={`/fhir-patient/${fHIRPatient.id}`} color="link" size="sm">
                      {fHIRPatient.id}
                    </Button>
                  </td>
                  <td>{fHIRPatient.fhirId}</td>
                  <td>{fHIRPatient.user ? fHIRPatient.user.login : ''}</td>
                  <td className="text-end">
                    <div className="btn-group flex-btn-group-container">
                      <Button tag={Link} to={`/fhir-patient/${fHIRPatient.id}`} color="info" size="sm" data-cy="entityDetailsButton">
                        <FontAwesomeIcon icon="eye" />{' '}
                        <span className="d-none d-md-inline">
                          <Translate contentKey="entity.action.view">View</Translate>
                        </span>
                      </Button>
                      <Button tag={Link} to={`/fhir-patient/${fHIRPatient.id}/edit`} color="primary" size="sm" data-cy="entityEditButton">
                        <FontAwesomeIcon icon="pencil-alt" />{' '}
                        <span className="d-none d-md-inline">
                          <Translate contentKey="entity.action.edit">Edit</Translate>
                        </span>
                      </Button>
                      <Button
                        tag={Link}
                        to={`/fhir-patient/${fHIRPatient.id}/delete`}
                        color="danger"
                        size="sm"
                        data-cy="entityDeleteButton"
                      >
                        <FontAwesomeIcon icon="trash" />{' '}
                        <span className="d-none d-md-inline">
                          <Translate contentKey="entity.action.delete">Delete</Translate>
                        </span>
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        ) : (
          !loading && (
            <div className="alert alert-warning">
              <Translate contentKey="openHealthManagerApp.fHIRPatient.home.notFound">No FHIR Patients found</Translate>
            </div>
          )
        )}
      </div>
    </div>
  );
};

export default FHIRPatient;
