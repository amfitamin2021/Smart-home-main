#!/bin/bash
set -e

# Создаем дополнительную базу данных для ThingsBoard
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE thingsboard;
    GRANT ALL PRIVILEGES ON DATABASE thingsboard TO postgres;
    
    CREATE DATABASE auth_service;
    GRANT ALL PRIVILEGES ON DATABASE auth_service TO postgres;
EOSQL

# Добавляем расширения и подготавливаем базу smarthome
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE EXTENSION IF NOT EXISTS pg_trgm;
    CREATE EXTENSION IF NOT EXISTS btree_gin;
EOSQL

echo "Базы данных инициализированы: smarthome, thingsboard, auth_service" 