import { resolveApiUrl } from './api-url';

export const environment = {
  production: false,
  apiUrl: resolveApiUrl('http://localhost:8080'),
};
