#!/usr/bin/env bash

# ==============================================================================
# Script de Teste & Criação de Monitores de Produto (Integração Java <-> Scraper)
# ==============================================================================

set -e

BASE_URL="${API_URL:-http://localhost:8080}"
USERNAME="${API_USER:-admin}"
PASSWORD="${API_PASS:-admin123}"

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}🚀 Teste de Criação de Monitores (Recorrência: A cada 1 minuto)${NC}"
echo -e "${BLUE}======================================================${NC}"
echo -e "Host API: ${YELLOW}${BASE_URL}${NC}"
echo -e "Usuário:  ${YELLOW}${USERNAME}${NC}"
echo ""

# 1. Autenticação (Login)
echo -e "${BLUE}[1/3] Autenticando na API para obter JWT Token...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"${USERNAME}\",
    \"password\": \"${PASSWORD}\"
  }")

TOKEN=$(echo "${LOGIN_RESPONSE}" | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "${TOKEN}" ]; then
  echo -e "${RED}❌ Falha na autenticação! Resposta da API:${NC}"
  echo "${LOGIN_RESPONSE}"
  exit 1
fi

echo -e "${GREEN}✅ Autenticado com sucesso! Token JWT obtido.${NC}"
echo ""

# 2. Criar Monitor 1 - SIMPLE (MacBook M1)
echo -e "${BLUE}[2/3] Criando Monitor 1: Teste Tool (MacBook M1)...${NC}"
MONITOR_NOTEBOOK_PAYLOAD='{
  "name": "MacBook M1 SP",
  "vendor": "OLX",
  "frequency": "EVERY_5_MINUTES",
  "analysisType": "SIMPLE",
  "analysisTypeFields": {
    "prompt": "Preciso de Macs m1 e que sejam especificamente m1 ou de apple silicon. Busque na descrição e nas especificações também"
  },
  "searchKeywords": [
    "macbook"
  ],
  "stateFilter": "sp",
  "requireDelivery": false
}'

RESPONSE_MONITOR_1=$(curl -s -X POST "${BASE_URL}/api/v1/product-monitors" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d "${MONITOR_NOTEBOOK_PAYLOAD}")

echo -e "${GREEN}✅ Resposta do Monitor 1:${NC}"
echo "${RESPONSE_MONITOR_1}"
echo ""

# 3. Criar Monitor 2 - SIMPLE (RTX 3060)
echo -e "${BLUE}[3/3] Criando Monitor 2: RTX 3060 SP (SIMPLE - EVERY_MINUTE)...${NC}"
MONITOR_GPU_PAYLOAD='{
  "name": "RTX 3060 SP",
  "vendor": "OLX",
  "frequency": "EVERY_MINUTE",
  "analysisType": "SIMPLE",
  "analysisTypeFields": {
    "prompt": "Avaliar se a placa é 12GB e em bom estado de conservação"
  },
  "searchKeywords": [
    "rtx 3060"
  ],
  "minPrice": 1000.00,
  "maxPrice": 1900.00,
  "stateFilter": "sp",
  "requireDelivery": true
}'

RESPONSE_MONITOR_2=$(curl -s -X POST "${BASE_URL}/api/v1/product-monitors" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d "${MONITOR_GPU_PAYLOAD}")

echo -e "${GREEN}✅ Resposta do Monitor 2:${NC}"
echo "${RESPONSE_MONITOR_2}"
echo ""
echo -e "${GREEN}======================================================${NC}"
echo -e "${GREEN}🎉 Monitores criados com sucesso!${NC}"
echo -e "${GREEN}Eles foram disparados imediatamente e continuarão rodando pelo Scheduler!${NC}"
echo -e "${GREEN}======================================================${NC}"
