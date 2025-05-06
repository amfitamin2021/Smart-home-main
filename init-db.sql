-- Создание основной базы данных для SmartHome
CREATE DATABASE IF NOT EXISTS smarthome;

-- Создание базы данных для ThingsBoard (используется отдельная БД)
CREATE DATABASE IF NOT EXISTS thingsboard;

-- Назначение прав
GRANT ALL PRIVILEGES ON DATABASE smarthome TO postgres;
GRANT ALL PRIVILEGES ON DATABASE thingsboard TO postgres; 