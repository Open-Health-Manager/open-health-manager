import { IUser } from 'app/shared/model/user.model';
import { IFHIRClient } from 'app/shared/model/fhir-client.model';

export interface IFHIRPatientConsent {
  id?: number;
  fhirResource?: string;
  user?: IUser;
  client?: IFHIRClient | null;
}

export const defaultValue: Readonly<IFHIRPatientConsent> = {};
