#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER thingsboard WITH PASSWORD 'postgres';
    GRANT ALL PRIVILEGES ON DATABASE thingsboard TO thingsboard;
    GRANT ALL PRIVILEGES ON SCHEMA public TO thingsboard;
    ALTER USER thingsboard WITH SUPERUSER;
EOSQL 