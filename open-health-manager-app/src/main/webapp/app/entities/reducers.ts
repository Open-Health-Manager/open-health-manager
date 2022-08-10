import fHIRPatient from 'app/entities/fhir-patient/fhir-patient.reducer';
import userDUA from 'app/entities/user-dua/user-dua.reducer';

import fHIRClient from 'app/entities/fhir-client/fhir-client.reducer';
import fHIRPatientConsent from 'app/entities/fhir-patient-consent/fhir-patient-consent.reducer';
/* jhipster-needle-add-reducer-import - JHipster will add reducer here */

const entitiesReducers = {
  fHIRPatient,
  userDUA,
  fHIRClient,
  fHIRPatientConsent,
  /* jhipster-needle-add-reducer-combine - JHipster will add reducer here */
};

export default entitiesReducers;
