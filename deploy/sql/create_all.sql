--
-- create_all.sql
--   SQL script to make everything for the inventory application in the database
--

--
-- Make sure that we have the UUID functions available in this database
--
create extension if not exists "uuid-ossp";

--
-- this is the table for the automotive data that we will have.
--
create table if not exists cars (
  as_of          timestamp with time zone not null,
  model_year     integer,
  manufacturer   varchar,
  quantity       integer,
  primary key (as_of, model_year, manufacturer)
);

--
-- open up the table for everyone to read
--
grant select on cars to public;


--
-- this is the table for the list of authorized users
--
create table if not exists users (
  email          varchar not null,
  primary key (email)
);

--
-- open up the table for everyone to read
--
grant select on users to public;
