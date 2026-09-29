(function (window) {
  window.env = window.env || {};
  // Deixe vazio em desenvolvimento para que o resolveApiUrl() detecte dinamicamente o host acessado (localhost ou Tailscale IP/Host)
  window.env.apiUrl = '';
})(this);
