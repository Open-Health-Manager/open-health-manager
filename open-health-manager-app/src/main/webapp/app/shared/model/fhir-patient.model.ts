import { IUser } from 'app/shared/model/user.model';

export interface IFHIRPatient {
  id?: number;
  fhirId?: string;
  user?: IUser;
}

export const defaultValue: Readonly<IFHIRPatient> = {};
