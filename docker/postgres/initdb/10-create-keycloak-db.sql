-- Creates a dedicated database for Keycloak alongside the banking database.
-- Runs only on first initialisation of the postgres data volume
-- (docker-entrypoint-initdb.d scripts are skipped when pgdata already exists).
-- If the volume predates Keycloak, create the DB manually:
--   docker compose exec postgres psql -U openfinova -d openfinova_local \
--     -c "CREATE DATABASE keycloak OWNER openfinova;"
SELECT 'CREATE DATABASE keycloak OWNER openfinova'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'keycloak')\gexec
