CREATE KEYSPACE IF NOT EXISTS fester
WITH replication = {
    'class': 'SimpleStrategy', 'replication_factor': '1'
};

CREATE TABLE IF NOT EXISTS fester.raw(
       time timestamp,
       key  varchar,
       name varchar,
       value double,
       PRIMARY KEY(time, key)
);

CREATE TABLE IF NOT EXISTS fester.rollups (
       time timestamp,
       rollup int,
       key  varchar,
       name varchar,
       value double,
       PRIMARY KEY(time, key)
);
