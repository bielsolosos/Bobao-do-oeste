# Observabilidade do Raspberry

O scraper roda diretamente no Raspberry por meio de `run_api.sh`, sem Docker. A configuracao coleta:

- CPU, memoria, swap, disco, temperatura, load e pressure do Raspberry.
- CPU, memoria, quantidade e estados do processo do scraper e de seus filhos Chromium.
- Logs JSON do scraper pelos arquivos gerenciados pelo PM2.
- Eventos de encerramento e reinicio registrados pelo daemon do PM2.
- Logs do kernel para diagnosticar OOM kill do host.

## 1. Executar o scraper pelo PM2

O PM2 ja e o supervisor do scraper, portanto nao deve existir uma segunda unit systemd para a aplicacao. O antigo `run_api.sh` usava `uvicorn --reload`, que inicia um watcher adicional e e destinado a desenvolvimento. O script agora usa um unico servidor sem reload e com `exec`, permitindo que o PM2 acompanhe e encerre o processo corretamente.

O arquivo `services/scraper/ecosystem.config.cjs` fixa o nome real do processo como `scrap` e desabilita prefixos de data do PM2 para preservar cada log JSON. Na primeira inicializacao:

```bash
cd /home/bielsolosos/projeto-scrap/services/scraper
uv sync
pm2 start ecosystem.config.cjs
pm2 save
pm2 status scrap
```

Se o script ja estiver registrado no PM2 com outro nome, pare e remova essa entrada antes de iniciar o ecosystem para nao executar duas instancias na porta `8001`.

Depois de atualizar o codigo ou as dependencias:

```bash
cd /home/bielsolosos/projeto-scrap/services/scraper
uv sync
pm2 reload ecosystem.config.cjs --update-env
pm2 save
```

Consulte o estado e os logs diretamente pelo PM2:

```bash
pm2 describe scrap
pm2 logs scrap
```

## 2. Permissoes do Alloy

O Alloy precisa atravessar o diretorio home e ler os logs do PM2. As ACLs evitam abrir esses arquivos para todos os usuarios:

```bash
sudo apt-get install --yes acl
sudo setfacl -m u:alloy:x /home/bielsolosos
sudo setfacl -m u:alloy:rx /home/bielsolosos/.pm2 /home/bielsolosos/.pm2/logs
sudo setfacl -m u:alloy:r /home/bielsolosos/.pm2/pm2.log /home/bielsolosos/.pm2/logs/scrap-*.log
sudo setfacl -d -m u:alloy:r-X /home/bielsolosos/.pm2 /home/bielsolosos/.pm2/logs
sudo usermod -aG adm,systemd-journal alloy
sudo systemctl restart alloy
```

O grupo `systemd-journal` continua necessario somente para os logs do kernel usados no diagnostico de OOM. Nao e necessario dar acesso ao Docker.

## 3. Configuracao do Alloy

O instalador do Grafana Cloud ja criou estes destinos em `/etc/alloy/config.alloy`:

```alloy
prometheus.remote_write "metrics_service"
loki.write "grafana_cloud_loki"
```

Anexe o conteudo de `deploy/observability/raspberry-host-and-logs.alloy` ao final desse arquivo. O snippet nao contem credenciais.

Valide e reinicie:

```bash
sudo alloy validate /etc/alloy/config.alloy
sudo systemctl restart alloy
sudo systemctl status alloy
sudo journalctl -u alloy --since "5 minutes ago" --no-pager
```

### Quando apenas os logs do kernel aparecem

O processo atual se chama `scrap` no PM2, portanto o Alloy procura `scrap-out.log` e `scrap-error.log`. Confirme caminho, conteudo e permissao:

```bash
pm2 status scrap
ls -la /home/bielsolosos/.pm2/logs/scrap-*.log
pm2 logs scrap --lines 20 --nostream
sudo -u alloy test -r /home/bielsolosos/.pm2/logs/scrap-out.log
sudo -u alloy test -r /home/bielsolosos/.pm2/logs/scrap-error.log
```

`tail_from_end=true` evita reenviar o historico. Depois de reiniciar o Alloy, gere uma linha nova e consulte novamente o Loki:

```bash
sudo systemctl restart alloy
curl --silent http://127.0.0.1:8001/health >/dev/null
sudo journalctl -u alloy --since "2 minutes ago" --no-pager
```

Use no Explore:

```logql
{application="projeto-scrap", service="scraper"}
```

Se os testes de leitura falharem, reaplique as ACLs da secao anterior. O arquivo `/home/bielsolosos/.pm2/pm2.log` gera o stream separado `{application="infrastructure", service="pm2"}`. Outros apps do PM2 nao sao coletados intencionalmente.

## 4. Como a memoria do Chromium e contabilizada

`prometheus.exporter.process` encontra o comando `uvicorn src.main:app` e usa `track_children=true`. Assim, o grupo `scraper` inclui o Python e os processos Chromium descendentes iniciados pelo Playwright.

Memoria residente agregada:

```promql
namedprocess_namegroup_memory_bytes{service="scraper-process",groupname="scraper",memtype="resident"}
```

CPU agregada:

```promql
sum(rate(namedprocess_namegroup_cpu_seconds_total{service="scraper-process",groupname="scraper"}[5m])) * 100
```

Quantidade de processos no grupo:

```promql
namedprocess_namegroup_num_procs{service="scraper-process",groupname="scraper"}
```

Quando o Chromium abre varios subprocessos, a quantidade e a memoria desse grupo aumentam.

## 5. Queries de diagnostico do host

Memoria disponivel:

```promql
100 * node_memory_MemAvailable_bytes{service="raspberry-host"}
  / node_memory_MemTotal_bytes{service="raspberry-host"}
```

OOM kills observados pelo kernel:

```promql
increase(node_vmstat_oom_kill{service="raspberry-host"}[1h])
```

CPU utilizada:

```promql
100 * (1 - avg by (host) (rate(node_cpu_seconds_total{service="raspberry-host",mode="idle"}[5m])))
```

Pressao de memoria:

```promql
rate(node_pressure_memory_waiting_seconds_total{service="raspberry-host"}[5m])
```

Swap utilizada:

```promql
node_memory_SwapTotal_bytes{service="raspberry-host"}
  - node_memory_SwapFree_bytes{service="raspberry-host"}
```

Quantidade atual de processos Python e Chromium no grupo:

```promql
namedprocess_namegroup_num_procs{service="scraper-process",groupname="scraper"}
```

O process exporter nao expoe o contador interno de reinicios do PM2. Consulte-o diretamente:

```bash
pm2 describe scrap
```

## 6. Queries de logs no Loki

Todos os logs do scraper:

```logql
{application="projeto-scrap", service="scraper"}
```

Somente erros:

```logql
{application="projeto-scrap", service="scraper", level="ERROR"} | json
```

Busca por job sem transformar seu ID em label:

```logql
{application="projeto-scrap", service="scraper"} |= "ID_DO_JOB" | json
```

Taxa de erros:

```logql
sum(count_over_time({application="projeto-scrap", service="scraper", level="ERROR"}[5m]))
```

OOM e kills reportados pelo kernel:

```logql
{application="infrastructure", service="kernel"} |~ "(?i)(out of memory|oom|killed process)"
```

Encerramentos e reinicios registrados pelo PM2:

```logql
{application="infrastructure", service="pm2"}
  |= "scrap"
  |~ "(?i)(exited|restart|online|stopped)"
```

## 7. Java no Coolify

O Alloy do Raspberry continua coletando `/actuator/prometheus` do Java pelo endereco HTTPS. Isso fornece JVM, HTTP, HikariCP e executors.

Logs, memoria total do container, CPU do container e OOM do Java exigem um Alloy na VPS, porque o Docker socket do Coolify so existe naquele host. A imagem Java ja possui labels estaveis e logs JSON para esse segundo agente.

Nao defina `max_memory_restart` no PM2 antes de obter uma linha de base. Depois, configure o limite acima do pico normal e crie um alerta antes de atingi-lo.

## 8. Dashboards e metricas do Spring AI

- `docs/grafana/observability-test-dashboard.json` cobre a visao geral de servicos, HTTP, JVM, HikariCP, Raspberry e processos.
- `docs/grafana/spring-ai-and-boot-dashboard.json` cobre chamadas, latencia, tokens, ferramentas do Spring AI e familias automaticas adicionais do Spring Boot.

O Spring AI 2.0.1 cria `gen_ai_client_token_usage_total` automaticamente quando o provedor devolve usage. A label `gen_ai_token_type` separa `input`, `output` e `total`. Essas series somente existem depois de uma chamada real ao Gemini terminar.

Micrometer mede tokens, nao dinheiro. Para calcular custo e necessario manter a tabela de precos do modelo, considerar cache e faixas de contexto, e multiplicar os tokens de entrada e saida pelas tarifas correspondentes.

PromQL para consumo no periodo selecionado:

```promql
sum by (gen_ai_token_type) (
  increase(gen_ai_client_token_usage_total{service="bi-engine"}[$__range])
)
```

PromQL para latencia media do modelo:

```promql
sum(rate(gen_ai_client_operation_seconds_sum{service="bi-engine"}[$__rate_interval]))
/
clamp_min(sum(rate(gen_ai_client_operation_seconds_count{service="bi-engine"}[$__rate_interval])), 0.000001)
```
