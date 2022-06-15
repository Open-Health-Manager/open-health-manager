import './home.scss';

import React from 'react';
import { Link } from 'react-router-dom';
import { Translate } from 'react-jhipster';
import { Row, Col, Alert } from 'reactstrap';

import { useAppSelector } from 'app/config/store';

export const Home = () => {
  const account = useAppSelector(state => state.authentication.account);

  return (
    <Row>
      <Col md="3" className="pad">
        <span className="hipster rounded" />
      </Col>
      <Col md="9">
        <h2>
          <Translate contentKey="home.title">Welcome to the Open Health Manager™!</Translate>
        </h2>
        <p className="lead">
          <Translate contentKey="home.subtitle">The Open Health Manager™ 
          is the reference implementation for the MITRE Health Manager Lab under the Living Health Lab initiative. 
          This project aims to radically flip the locus of health data from organizations to individuals, 
          promoting individual agency in personal health through primary self-care and engagement with their care team.</Translate>
        </p>
        {account?.login ? (
          <div>
            <Alert color="success">
              <Translate contentKey="home.logged.message" interpolate={{ username: account.login }}>
                You are logged in as user {account.login}.
              </Translate>
            </Alert>
          </div>
        ) : (
          <div>
            <Alert color="warning">
              <Translate contentKey="global.messages.info.authenticated.prefix">If you want to </Translate>

              <Link to="/login" className="alert-link">
                <Translate contentKey="global.messages.info.authenticated.link"> sign in</Translate>
              </Link>
              <Translate contentKey="global.messages.info.authenticated.suffix">
                , you can try the default accounts:
                <br />- Administrator (login=&quot;admin&quot; and password=&quot;admin&quot;)
                <br />- User (login=&quot;user&quot; and password=&quot;user&quot;).
              </Translate>
            </Alert>

            <Alert color="warning">
              <Translate contentKey="global.messages.info.register.noaccount">You do not have an account yet?</Translate>&nbsp;
              <Link to="/account/register" className="alert-link">
                <Translate contentKey="global.messages.info.register.link">Register a new account</Translate>
              </Link>
            </Alert>
          </div>
        )}
        <p>
          <Translate contentKey="home.linkinfo">
            To access the Open Health Manager™ HAPI FHIR server, visit the following links:
          </Translate>
        </p>
        <ul>
            <li>
                <a href="tester/home" target="_blank" rel="noopener noreferrer">
                  <Translate contentKey="home.link.testoverlay">HAPI FHIR Server Test Overlay</Translate>
                </a>
            </li>
            <li>
                <a href="fhir/swagger-ui/" target="_blank" rel="noopener noreferrer">
                  <Translate contentKey="home.link.swaggerui">HAPI FHIR Server Swagger UI</Translate>
                </a>
            </li>
        </ul>       
        <p>
          <Translate contentKey="home.question">If you have any questions about the Open Health Manager™, visit our </Translate>
          <a href="https://github.com/Open-Health-Manager/open-health-manager" target="_blank" rel="noopener noreferrer">
              <Translate contentKey="home.link.github">GitHub repository</Translate>
          </a>
        </p>
        <p>
          <Translate contentKey="home.additional.info">
            This project is built on the HAPI FHIR Starter Project that is used to deploy a FHIR server using HAPI FHIR JPA. It&apos;s GitHub repository
            can be found 
          </Translate>
          <a href = "https://github.com/hapifhir/hapi-fhir-jpaserver-starter" target="_blank" rel="noopener noreferrer">
            <Translate contentKey="home.link.hapifhirserver">
              here
            </Translate>
          </a>
        </p>
      </Col>
    </Row>
  );
};

export default Home;
