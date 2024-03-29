import React from 'react';
import MenuItem from 'app/shared/layout/menus/menu-item';
import { DropdownItem } from 'reactstrap';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { NavDropdown } from './menu-components';
import { Translate, translate } from 'react-jhipster';

const adminMenuItems = () => (
  <>
    <MenuItem icon="users" to="/admin/user-management">
      <Translate contentKey="global.menu.admin.userManagement">User management</Translate>
    </MenuItem>
    <MenuItem icon="asterisk" to="/fhir-patient">
        <Translate contentKey="global.menu.entities.fhirPatient" />
    </MenuItem>
    <MenuItem icon="asterisk" to="/user-dua">
        <Translate contentKey="global.menu.entities.userDua" />
    </MenuItem>  
    <MenuItem icon="asterisk" to="/fhir-client">
        <Translate contentKey="global.menu.entities.fhirClient" />
    </MenuItem>
    <MenuItem icon="asterisk" to="/fhir-patient-consent">
      <Translate contentKey="global.menu.entities.fhirPatientConsent" />
    </MenuItem>
    <MenuItem icon="tachometer-alt" to="/admin/metrics">
      <Translate contentKey="global.menu.admin.metrics">Metrics</Translate>
    </MenuItem>
    <MenuItem icon="heart" to="/admin/health">
      <Translate contentKey="global.menu.admin.health">Health</Translate>
    </MenuItem>
    <MenuItem icon="cogs" to="/admin/configuration">
      <Translate contentKey="global.menu.admin.configuration">Configuration</Translate>
    </MenuItem>
    <MenuItem icon="tasks" to="/admin/logs">
      <Translate contentKey="global.menu.admin.logs">Logs</Translate>
    </MenuItem>   
    {/* jhipster-needle-add-element-to-admin-menu - JHipster will add entities to the admin menu here */}
  </>
);

const openAPIItem = () => (
  <MenuItem icon="book" to="/admin/docs">
    <Translate contentKey="global.menu.admin.apidocs">API</Translate>
  </MenuItem>
);

const databaseItem = () => (
  <DropdownItem tag="a" href="./h2-console/" target="_tab">
    <FontAwesomeIcon icon="database" fixedWidth /> <Translate contentKey="global.menu.admin.database">Database</Translate>
  </DropdownItem>
);

const openTestUIItem = () => (
  <DropdownItem tag="a" href="tester/home" target="_tab">
    <FontAwesomeIcon icon="window-restore" fixedWidth /> <Translate contentKey="global.menu.admin.testui">HAPI Testpage Overlay</Translate>
  </DropdownItem>
);

const openSwaggerUIItem = () => (
  <DropdownItem tag="a" href="fhir/swagger-ui/" target="_tab">
    <FontAwesomeIcon icon="fire" fixedWidth /> <Translate contentKey="global.menu.admin.swaggerui">HAPI Swagger UI</Translate>
  </DropdownItem>
);

export const AdminMenu = ({ showOpenAPI, showDatabase }) => (
  <NavDropdown icon="users-cog" name={translate('global.menu.admin.main')} id="admin-menu" data-cy="adminMenu">
    {adminMenuItems()}
    {showOpenAPI && openAPIItem()}

    {showDatabase && databaseItem()}

    {openTestUIItem()}

    {openSwaggerUIItem()}
  </NavDropdown>
);

export default AdminMenu;
