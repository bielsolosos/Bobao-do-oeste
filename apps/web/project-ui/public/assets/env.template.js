(function(window) {
  window.env = window.env || {};
  // Variável que será injetada pelo Docker/Coolify no runtime
  window.env.apiUrl = '${API_URL}';
})(this);
