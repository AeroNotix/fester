CREATE KEYSPACE IF NOT EXISTS fester
WITH replication = {
    'class': 'SimpleStrategy', 'replication_factor': '1'
};

CREATE TABLE IF NOT EXISTS fester.raw (
       key  varchar PRIMARY KEY,
       name varchar,
       value double
);
