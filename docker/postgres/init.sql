-- PostgreSQL initialization script for Hitorro
-- This script runs automatically when the postgres container starts for the first time

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Grant necessary permissions
GRANT ALL PRIVILEGES ON DATABASE hitorrodb TO hitorro;

-- Create schema if needed (optional, for organization)
-- CREATE SCHEMA IF NOT EXISTS hitorro AUTHORIZATION hitorro;

-- Set default search path (optional)
-- ALTER DATABASE hitorrodb SET search_path TO hitorro,public;

-- Performance tuning settings (adjust based on your needs)
ALTER SYSTEM SET shared_buffers = '256MB';
ALTER SYSTEM SET effective_cache_size = '1GB';
ALTER SYSTEM SET maintenance_work_mem = '64MB';
ALTER SYSTEM SET checkpoint_completion_target = 0.9;
ALTER SYSTEM SET wal_buffers = '16MB';
ALTER SYSTEM SET default_statistics_target = 100;
ALTER SYSTEM SET random_page_cost = 1.1;
ALTER SYSTEM SET effective_io_concurrency = 200;
ALTER SYSTEM SET work_mem = '4MB';
ALTER SYSTEM SET min_wal_size = '1GB';
ALTER SYSTEM SET max_wal_size = '4GB';

-- Log settings for debugging (optional, disable in production)
-- ALTER SYSTEM SET log_statement = 'all';
-- ALTER SYSTEM SET log_duration = on;

-- Reload configuration (note: container will restart anyway)
SELECT pg_reload_conf();
