export const environment = {
  production: false,
  apiUrl: (window as any).env?.apiUrl || 'http://localhost:8080/api/v1'
};
