import axios from 'axios';
import { createAsyncThunk, isFulfilled, isPending, isRejected } from '@reduxjs/toolkit';

import { cleanEntity } from 'app/shared/util/entity-utils';
import { IQueryParams, createEntitySlice, EntityState, serializeAxiosError } from 'app/shared/reducers/reducer.utils';
import { IFHIRPatientConsent, defaultValue } from 'app/shared/model/fhir-patient-consent.model';

const initialState: EntityState<IFHIRPatientConsent> = {
  loading: false,
  errorMessage: null,
  entities: [],
  entity: defaultValue,
  updating: false,
  totalItems: 0,
  updateSuccess: false,
};

const apiUrl = 'api/fhir-patient-consents';

// Actions

export const getEntities = createAsyncThunk('fHIRPatientConsent/fetch_entity_list', async ({ page, size, sort }: IQueryParams) => {
  const requestUrl = `${apiUrl}${sort ? `?page=${page}&size=${size}&sort=${sort}&` : '?'}cacheBuster=${new Date().getTime()}`;
  return axios.get<IFHIRPatientConsent[]>(requestUrl);
});

export const getEntity = createAsyncThunk(
  'fHIRPatientConsent/fetch_entity',
  async (id: string | number) => {
    const requestUrl = `${apiUrl}/${id}`;
    return axios.get<IFHIRPatientConsent>(requestUrl);
  },
  { serializeError: serializeAxiosError }
);

// slice

export const FHIRPatientConsentSlice = createEntitySlice({
  name: 'fHIRPatientConsent',
  initialState,
  extraReducers(builder) {
    builder
      .addCase(getEntity.fulfilled, (state, action) => {
        state.loading = false;
        state.entity = action.payload.data;
      })
      .addMatcher(isFulfilled(getEntities), (state, action) => {
        const { data, headers } = action.payload;

        return {
          ...state,
          loading: false,
          entities: data,
          totalItems: parseInt(headers['x-total-count'], 10),
        };
      })
      .addMatcher(isPending(getEntities, getEntity), state => {
        state.errorMessage = null;
        state.updateSuccess = false;
        state.loading = true;
      });
  },
});

export const { reset } = FHIRPatientConsentSlice.actions;

// Reducer
export default FHIRPatientConsentSlice.reducer;
