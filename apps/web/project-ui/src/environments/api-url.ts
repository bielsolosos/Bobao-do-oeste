const API_PREFIX = '/api/v1';

type RuntimeWindow = Window & {
  env?: {
    apiUrl?: string;
  };
};

export function resolveApiUrl(fallbackBaseUrl?: string): string {
  const runtimeWindow = typeof window !== 'undefined' ? (window as RuntimeWindow) : undefined;
  const configuredBaseUrl = runtimeWindow?.env?.apiUrl;
  let rawBaseUrl =
    configuredBaseUrl && !configuredBaseUrl.includes('${') && configuredBaseUrl.trim() !== ''
      ? configuredBaseUrl
      : fallbackBaseUrl;

  // Se estiver no browser e acessando por IP ou hostname remoto (Tailscale, etc), ajusta a base URL
  if (
    runtimeWindow?.location?.hostname &&
    runtimeWindow.location.hostname !== 'localhost' &&
    runtimeWindow.location.hostname !== '127.0.0.1'
  ) {
    if (
      !configuredBaseUrl ||
      configuredBaseUrl.trim() === '' ||
      rawBaseUrl?.includes('localhost') ||
      rawBaseUrl?.includes('127.0.0.1')
    ) {
      const port = '8080';
      const protocol = runtimeWindow.location.protocol || 'http:';
      rawBaseUrl = `${protocol}//${runtimeWindow.location.hostname}:${port}`;
    }
  }

  if (!rawBaseUrl) {
    throw new Error('API_URL não foi configurada para o ambiente de produção.');
  }

  const normalizedBaseUrl = rawBaseUrl.replace(/\/+$/, '').replace(/\/api\/v1$/, '');

  if (!/^https?:\/\//.test(normalizedBaseUrl)) {
    throw new Error('API_URL deve ser uma URL absoluta iniciada por http:// ou https://.');
  }

  return `${normalizedBaseUrl}${API_PREFIX}`;
}
