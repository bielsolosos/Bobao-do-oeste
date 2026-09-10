const runtimeApiUrl = (window as Window & { env?: { apiUrl?: string } }).env?.apiUrl;

if (!runtimeApiUrl) {
  throw new Error('API_URL não foi configurada para o ambiente de produção.');
}

export const environment = {
  production: true,
  apiUrl: runtimeApiUrl,
};
