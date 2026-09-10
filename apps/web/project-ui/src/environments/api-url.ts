const API_PREFIX = '/api/v1';

type RuntimeWindow = Window & {
  env?: {
    apiUrl?: string;
  };
};

export function resolveApiUrl(fallbackBaseUrl?: string): string {
  const configuredBaseUrl = (window as RuntimeWindow).env?.apiUrl;
  const rawBaseUrl = configuredBaseUrl && !configuredBaseUrl.includes('${')
    ? configuredBaseUrl
    : fallbackBaseUrl;

  if (!rawBaseUrl) {
    throw new Error('API_URL não foi configurada para o ambiente de produção.');
  }

  const normalizedBaseUrl = rawBaseUrl
    .replace(/\/+$/, '')
    .replace(/\/api\/v1$/, '');

  if (!/^https?:\/\//.test(normalizedBaseUrl)) {
    throw new Error('API_URL deve ser uma URL absoluta iniciada por http:// ou https://.');
  }

  return `${normalizedBaseUrl}${API_PREFIX}`;
}
