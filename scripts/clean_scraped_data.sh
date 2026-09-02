#!/usr/bin/env bash

# ==============================================================================
# Script para Limpar Dados de Anúncios, Logs de IA e Execuções de Scraping
# Preserva os usuários e os monitores de produto cadastrados.
# ==============================================================================

set -e

CONTAINER_NAME="marketplace_postgres"
DB_NAME="marketplace_bi"
DB_USER="postgres"

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}🧹 Limpeza de Dados Coletados & Logs de IA (PostgreSQL)${NC}"
echo -e "${BLUE}======================================================${NC}"

# Detecta podman ou docker
if command -v podman &> /dev/null; then
  CONTAINER_CLI="podman"
elif command -v docker &> /dev/null; then
  CONTAINER_CLI="docker"
else
  echo -e "${RED}❌ Nem podman nem docker foram encontrados no sistema.${NC}"
  exit 1
fi

echo -e "Utilizando container engine: ${YELLOW}${CONTAINER_CLI}${NC}"
echo -e "Container alvo:              ${YELLOW}${CONTAINER_NAME}${NC}"
echo -e "Banco de dados:              ${YELLOW}${DB_NAME}${NC}"
echo ""

echo -e "${BLUE}Truncando tabelas (scraped_listings, ai_analysis_logs, scraping_executions, webhook_events)...${NC}"

${CONTAINER_CLI} exec -i "${CONTAINER_NAME}" psql -U "${DB_USER}" -d "${DB_NAME}" -c \
  "TRUNCATE TABLE ai_analysis_logs, scraped_listings, scraping_executions, webhook_events CASCADE;"

echo ""
echo -e "${GREEN}✅ Tabelas limpas com sucesso!${NC}"
echo ""

echo -e "${BLUE}📊 Status atual das tabelas:${NC}"
${CONTAINER_CLI} exec -i "${CONTAINER_NAME}" psql -U "${DB_USER}" -d "${DB_NAME}" -c \
  "SELECT 
     (SELECT count(*) FROM product_monitors) AS monitores_ativos,
     (SELECT count(*) FROM scraped_listings) AS anuncios_coletados,
     (SELECT count(*) FROM ai_analysis_logs) AS logs_ia,
     (SELECT count(*) FROM scraping_executions) AS execucoes;"

echo ""
echo -e "${GREEN}🎉 Pronto para reiniciar coletas e testar novas análises!${NC}"
