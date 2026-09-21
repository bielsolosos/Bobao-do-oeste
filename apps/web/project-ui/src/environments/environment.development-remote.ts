import { resolveApiUrl } from './api-url';

export const environment = {
  production: false,
  apiUrl: resolveApiUrl('http://100.86.218.8:8080'),
};
