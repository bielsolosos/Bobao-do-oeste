(function(window) {
  window.env = window.env || {};
  // Host da API injetado pelo Docker/Coolify; /api/v1 é acrescentado pelo frontend.
  window.env.apiUrl = '${API_URL}';
})(this);
