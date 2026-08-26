-- ==============================================================================
-- 1. TABELA DEDICADA DE WEBHOOKS (Porta de Entrada & Auditoria)
-- ==============================================================================
CREATE TABLE webhook_events (
    id UUID PRIMARY KEY,
    request_id VARCHAR(255) NOT NULL UNIQUE,
    job_id VARCHAR(255),
    source VARCHAR(50) NOT NULL DEFAULT 'SCRAPER_PYTHON',
    status VARCHAR(50) NOT NULL DEFAULT 'RECEIVED',
    headers JSONB DEFAULT '{}'::jsonb,
    raw_payload JSONB NOT NULL,
    error_message TEXT,
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_webhook_events_request_id ON webhook_events(request_id);
CREATE INDEX idx_webhook_events_status ON webhook_events(status);
CREATE INDEX idx_webhook_events_created_at ON webhook_events(created_at);


-- ==============================================================================
-- 2. TABELA PRINCIPAL: PRODUCT_MONITORS
-- ==============================================================================
CREATE TABLE product_monitors (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    analysis_type VARCHAR(50) NOT NULL DEFAULT 'SIMPLE',
    target_vendor VARCHAR(50) NOT NULL DEFAULT 'OLX',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    cron_expression VARCHAR(50),
    expected_specs JSONB DEFAULT '{}'::jsonb,
    last_scraped_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_product_monitors_user_id ON product_monitors(user_id);
CREATE INDEX idx_product_monitors_active ON product_monitors(is_active);
CREATE INDEX idx_product_monitors_analysis_type ON product_monitors(analysis_type);


-- ==============================================================================
-- 3. QUERIES DE BUSCA DO MONITOR: MONITOR_SEARCH_QUERIES
-- ==============================================================================
CREATE TABLE monitor_search_queries (
    id UUID PRIMARY KEY,
    product_monitor_id UUID NOT NULL REFERENCES product_monitors(id) ON DELETE CASCADE,
    query_term VARCHAR(200) NOT NULL,
    min_price NUMERIC(12, 2),
    max_price NUMERIC(12, 2),
    state_filter VARCHAR(10),
    region_filter VARCHAR(50),
    category_slug VARCHAR(100),
    require_delivery BOOLEAN NOT NULL DEFAULT FALSE,
    max_pages INT NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_search_queries_monitor_id ON monitor_search_queries(product_monitor_id);
CREATE INDEX idx_search_queries_active ON monitor_search_queries(is_active);


-- ==============================================================================
-- 4. EXECUÇÕES PROCESSADAS: SCRAPING_EXECUTIONS
-- ==============================================================================
CREATE TABLE scraping_executions (
    id UUID PRIMARY KEY,
    webhook_event_id UUID REFERENCES webhook_events(id) ON DELETE SET NULL,
    product_monitor_id UUID NOT NULL REFERENCES product_monitors(id) ON DELETE CASCADE,
    search_query_id UUID REFERENCES monitor_search_queries(id) ON DELETE SET NULL,
    vendor VARCHAR(50) NOT NULL DEFAULT 'OLX',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    total_found INT NOT NULL DEFAULT 0,
    new_items_count INT NOT NULL DEFAULT 0,
    duration_ms INT,
    used_fallback BOOLEAN NOT NULL DEFAULT FALSE,
    error_message TEXT,
    executed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_scraping_exec_webhook_id ON scraping_executions(webhook_event_id);
CREATE INDEX idx_scraping_exec_monitor_id ON scraping_executions(product_monitor_id);
CREATE INDEX idx_scraping_exec_query_id ON scraping_executions(search_query_id);
CREATE INDEX idx_scraping_exec_status ON scraping_executions(status);


-- ==============================================================================
-- 5. ANÚNCIOS COLETADOS: SCRAPED_LISTINGS
-- ==============================================================================
CREATE TABLE scraped_listings (
    id UUID PRIMARY KEY,
    product_monitor_id UUID NOT NULL REFERENCES product_monitors(id) ON DELETE CASCADE,
    last_execution_id UUID REFERENCES scraping_executions(id) ON DELETE SET NULL,
    vendor VARCHAR(50) NOT NULL DEFAULT 'OLX',
    vendor_listing_id VARCHAR(100) NOT NULL,
    title VARCHAR(300) NOT NULL,
    url TEXT NOT NULL,
    description TEXT,
    current_price NUMERIC(12, 2) NOT NULL,
    original_price NUMERIC(12, 2),
    
    -- Localização & Entrega
    state VARCHAR(10),
    city VARCHAR(100),
    neighborhood VARCHAR(100),
    has_delivery BOOLEAN NOT NULL DEFAULT FALSE,
    delivery_type VARCHAR(50),
    images JSONB DEFAULT '[]'::jsonb,
    
    -- Lógica de Match & Specs (Dashboard)
    match_tier VARCHAR(50) NOT NULL DEFAULT 'NONE',
    match_score NUMERIC(5, 2) DEFAULT 0.00,
    extracted_specs JSONB DEFAULT '{}'::jsonb,
    
    -- Ciclo de vida
    published_at TIMESTAMP WITH TIME ZONE,
    first_seen_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_monitor_vendor_listing UNIQUE (product_monitor_id, vendor, vendor_listing_id)
);

CREATE INDEX idx_scraped_listings_monitor_id ON scraped_listings(product_monitor_id);
CREATE INDEX idx_scraped_listings_vendor_id ON scraped_listings(vendor, vendor_listing_id);
CREATE INDEX idx_scraped_listings_match_tier ON scraped_listings(match_tier);
CREATE INDEX idx_scraped_listings_price ON scraped_listings(current_price);
CREATE INDEX idx_scraped_listings_extracted_specs ON scraped_listings USING GIN (extracted_specs);

