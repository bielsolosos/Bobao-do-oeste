import { resolveApiUrl } from './api-url';

export const environment = {
  production: true,
  apiUrl: resolveApiUrl(),
};
