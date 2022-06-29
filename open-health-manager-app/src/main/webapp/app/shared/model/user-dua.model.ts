import dayjs from 'dayjs';
import { IUser } from 'app/shared/model/user.model';

export interface IUserDUA {
  id?: number;
  active?: boolean;
  version?: string;
  ageAttested?: boolean;
  activeDate?: string;
  revocationDate?: string | null;
  user?: IUser;
}

export const defaultValue: Readonly<IUserDUA> = {
  active: false,
  ageAttested: false,
};
