import { ClientDirection } from 'app/shared/model/enumerations/client-direction.model';

export interface IFHIRClient {
  id?: number;
  name?: string;
  displayName?: string;
  uri?: string | null;
  fhirOrganizationId?: string;
  clientDirection?: ClientDirection | null;
}

export const defaultValue: Readonly<IFHIRClient> = {};
