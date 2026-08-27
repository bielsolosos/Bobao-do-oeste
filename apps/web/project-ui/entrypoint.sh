#!/bin/sh
# Substitui a variável ${API_URL} no template e joga pro arquivo final de JS servido pelo Nginx
envsubst < /usr/share/nginx/html/assets/env.template.js > /usr/share/nginx/html/assets/env.js
# Inicia o nginx
exec nginx -g "daemon off;"
