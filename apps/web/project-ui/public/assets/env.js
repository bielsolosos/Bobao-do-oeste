(function(window) {
  window.env = window.env || {};
  // Host padrão local. O frontend acrescenta /api/v1 centralmente.
  window.env.apiUrl = 'http://localhost:8080';
})(this);
