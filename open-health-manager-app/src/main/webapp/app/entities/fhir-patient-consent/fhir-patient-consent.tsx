import React, { useState, useEffect } from 'react';
import { Link, RouteComponentProps } from 'react-router-dom';
import { Button, Table } from 'reactstrap';
import { byteSize, Translate, getSortState, JhiPagination, JhiItemCount } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { APP_DATE_FORMAT, APP_LOCAL_DATE_FORMAT } from 'app/config/constants';
import { ASC, DESC, ITEMS_PER_PAGE, SORT } from 'app/shared/util/pagination.constants';
import { overridePaginationStateWithQueryParams } from 'app/shared/util/entity-utils';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { IFHIRPatientConsent } from 'app/shared/model/fhir-patient-consent.model';
import { getEntities } from './fhir-patient-consent.reducer';

export const FHIRPatientConsent = (props: RouteComponentProps<{ url: string }>) => {
  const dispatch = useAppDispatch();

  const [paginationState, setPaginationState] = useState(
    overridePaginationStateWithQueryParams(getSortState(props.location, ITEMS_PER_PAGE, 'id'), props.location.search)
  );

  const fHIRPatientConsentList = useAppSelector(state => state.fHIRPatientConsent.entities);
  const loading = useAppSelector(state => state.fHIRPatientConsent.loading);
  const totalItems = useAppSelector(state => state.fHIRPatientConsent.totalItems);

  const getAllEntities = () => {
    dispatch(
      getEntities({
        page: paginationState.activePage - 1,
        size: paginationState.itemsPerPage,
        sort: `${paginationState.sort},${paginationState.order}`,
      })
    );
  };

  const sortEntities = () => {
    getAllEntities();
    const endURL = `?page=${paginationState.activePage}&sort=${paginationState.sort},${paginationState.order}`;
    if (props.location.search !== endURL) {
      props.history.push(`${props.location.pathname}${endURL}`);
    }
  };

  useEffect(() => {
    sortEntities();
  }, [paginationState.activePage, paginationState.order, paginationState.sort]);

  useEffect(() => {
    const params = new URLSearchParams(props.location.search);
    const page = params.get('page');
    const sort = params.get(SORT);
    if (page && sort) {
      const sortSplit = sort.split(',');
      setPaginationState({
        ...paginationState,
        activePage: +page,
        sort: sortSplit[0],
        order: sortSplit[1],
      });
    }
  }, [props.location.search]);

  const sort = p => () => {
    setPaginationState({
      ...paginationState,
      order: paginationState.order === ASC ? DESC : ASC,
      sort: p,
    });
  };

  const handlePagination = currentPage =>
    setPaginationState({
      ...paginationState,
      activePage: currentPage,
    });

  const handleSyncList = () => {
    sortEntities();
  };

  const { match } = props;

  return (
    <div>
      <h2 id="fhir-patient-consent-heading" data-cy="FHIRPatientConsentHeading">
        <Translate contentKey="openHealthManagerApp.fHIRPatientConsent.home.title">FHIR Patient Consents</Translate>
        <div className="d-flex justify-content-end">
          <Button className="me-2" color="info" onClick={handleSyncList} disabled={loading}>
            <FontAwesomeIcon icon="sync" spin={loading} />{' '}
            <Translate contentKey="openHealthManagerApp.fHIRPatientConsent.home.refreshListLabel">Refresh List</Translate>
          </Button>
          <Link
            to="/fhir-patient-consent/new"
            className="btn btn-primary jh-create-entity"
            id="jh-create-entity"
            data-cy="entityCreateButton"
          >
            <FontAwesomeIcon icon="plus" />
            &nbsp;
            <Translate contentKey="openHealthManagerApp.fHIRPatientConsent.home.createLabel">Create new FHIR Patient Consent</Translate>
          </Link>
        </div>
      </h2>
      <div className="table-responsive">
        {fHIRPatientConsentList && fHIRPatientConsentList.length > 0 ? (
          <Table responsive>
            <thead>
              <tr>
                <th className="hand" onClick={sort('id')}>
                  <Translate contentKey="openHealthManagerApp.fHIRPatientConsent.id">ID</Translate> <FontAwesomeIcon icon="sort" />
                </th>
                <th className="hand" onClick={sort('approve')}>
                  <Translate contentKey="openHealthManagerApp.fHIRPatientConsent.approve">Approve</Translate>{' '}
                  <FontAwesomeIcon icon="sort" />
                </th>
                <th className="hand" onClick={sort('fhirResource')}>
                  <Translate contentKey="openHealthManagerApp.fHIRPatientConsent.fhirResource">Fhir Resource</Translate>{' '}
                  <FontAwesomeIcon icon="sort" />
                </th>
                <th>
                  <Translate contentKey="openHealthManagerApp.fHIRPatientConsent.user">User</Translate> <FontAwesomeIcon icon="sort" />
                </th>
                <th>
                  <Translate contentKey="openHealthManagerApp.fHIRPatientConsent.client">Client</Translate> <FontAwesomeIcon icon="sort" />
                </th>
                <th />
              </tr>
            </thead>
            <tbody>
              {fHIRPatientConsentList.map((fHIRPatientConsent, i) => (
                <tr key={`entity-${i}`} data-cy="entityTable">
                  <td>
                    <Button tag={Link} to={`/fhir-patient-consent/${fHIRPatientConsent.id}`} color="link" size="sm">
                      {fHIRPatientConsent.id}
                    </Button>
                  </td>
                  <td>{fHIRPatientConsent.approve ? 'true' : 'false'}</td>
                  <td>{fHIRPatientConsent.fhirResource}</td>
                  <td>{fHIRPatientConsent.user ? fHIRPatientConsent.user.login : ''}</td>
                  <td>
                    {fHIRPatientConsent.client ? (
                      <Link to={`/fhir-client/${fHIRPatientConsent.client.id}`}>{fHIRPatientConsent.client.name}</Link>
                    ) : (
                      ''
                    )}
                  </td>
                  <td className="text-end">
                    <div className="btn-group flex-btn-group-container">
                      <Button
                        tag={Link}
                        to={`/fhir-patient-consent/${fHIRPatientConsent.id}`}
                        color="info"
                        size="sm"
                        data-cy="entityDetailsButton"
                      >
                        <FontAwesomeIcon icon="eye" />{' '}
                        <span className="d-none d-md-inline">
                          <Translate contentKey="entity.action.view">View</Translate>
                        </span>
                      </Button>
                      <Button
                        tag={Link}
                        to={`/fhir-patient-consent/${fHIRPatientConsent.id}/edit?page=${paginationState.activePage}&sort=${paginationState.sort},${paginationState.order}`}
                        color="primary"
                        size="sm"
                        data-cy="entityEditButton"
                      >
                        <FontAwesomeIcon icon="pencil-alt" />{' '}
                        <span className="d-none d-md-inline">
                          <Translate contentKey="entity.action.edit">Edit</Translate>
                        </span>
                      </Button>
                      <Button
                        tag={Link}
                        to={`/fhir-patient-consent/${fHIRPatientConsent.id}/delete?page=${paginationState.activePage}&sort=${paginationState.sort},${paginationState.order}`}
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
              <Translate contentKey="openHealthManagerApp.fHIRPatientConsent.home.notFound">No FHIR Patient Consents found</Translate>
            </div>
          )
        )}
      </div>
      {totalItems ? (
        <div className={fHIRPatientConsentList && fHIRPatientConsentList.length > 0 ? '' : 'd-none'}>
          <div className="justify-content-center d-flex">
            <JhiItemCount page={paginationState.activePage} total={totalItems} itemsPerPage={paginationState.itemsPerPage} i18nEnabled />
          </div>
          <div className="justify-content-center d-flex">
            <JhiPagination
              activePage={paginationState.activePage}
              onSelect={handlePagination}
              maxButtons={5}
              itemsPerPage={paginationState.itemsPerPage}
              totalItems={totalItems}
            />
          </div>
        </div>
      ) : (
        ''
      )}
    </div>
  );
};

export default FHIRPatientConsent;
