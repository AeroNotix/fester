DROP KEYSPACE IF EXISTS fester;

CREATE KEYSPACE IF NOT EXISTS fester
WITH replication = {
    'class': 'SimpleStrategy', 'replication_factor': '1'
};

USE fester;

CREATE TABLE IF NOT EXISTS raw(
       time timestamp,
       key  varchar,
       value double,
       PRIMARY KEY(time, key)
);

CREATE TABLE IF NOT EXISTS rollups (
       time timestamp,
       rollup int,
       key  varchar,
       value double,
       PRIMARY KEY(time, rollup, key)
);
