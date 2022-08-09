import { IUser } from 'app/shared/model/user.model';
import { IFHIRClient } from 'app/shared/model/fhir-client.model';

export interface IFHIRPatientConsent {
  id?: number;
  approve?: boolean | null;
  fhirResource?: string | null;
  user?: IUser;
  client?: IFHIRClient | null;
}

export const defaultValue: Readonly<IFHIRPatientConsent> = {
  approve: false,
};
