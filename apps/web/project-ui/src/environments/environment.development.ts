export const environment = {
  production: false,
  apiUrl: (window as Window & { env?: { apiUrl?: string } }).env?.apiUrl || 'http://localhost:8080/api/v1'
};
