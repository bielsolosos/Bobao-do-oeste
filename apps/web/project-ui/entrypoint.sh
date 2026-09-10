#!/bin/sh
set -eu

: "${API_URL:?API_URL é obrigatória e deve apontar para o host da API}"

case "$API_URL" in
  http://*|https://*) ;;
  *)
    echo "API_URL inválida: use uma URL absoluta iniciada por http:// ou https://" >&2
    exit 1
    ;;
esac

API_URL="${API_URL%/}"
export API_URL

# Substitui a variável ${API_URL} no template e gera o arquivo servido pelo Nginx.
envsubst < /usr/share/nginx/html/assets/env.template.js > /usr/share/nginx/html/assets/env.js
exec nginx -g "daemon off;"
