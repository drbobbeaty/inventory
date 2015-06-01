--
-- seed_cars.sql
--   This just adds the default starting set of cars to the database
--

insert into cars(as_of, model_year, manufacturer, quantity)
  values(now(), 2008, 'Kia', 100);
insert into cars(as_of, model_year, manufacturer, quantity)
  values(now(), 2009, 'Kia', 200);
insert into cars(as_of, model_year, manufacturer, quantity)
  values(now(), 2010, 'Kia', 300);

insert into cars(as_of, model_year, manufacturer, quantity)
  values(now(), 2008, 'Nissan', 11);
insert into cars(as_of, model_year, manufacturer, quantity)
  values(now(), 2009, 'Nissan', 11);
insert into cars(as_of, model_year, manufacturer, quantity)
  values(now(), 2010, 'Nissan', 15);

insert into cars(as_of, model_year, manufacturer, quantity)
  values(now(), 2008, 'Toyota', 12);
insert into cars(as_of, model_year, manufacturer, quantity)
  values(now(), 2009, 'Toyota', 14);
insert into cars(as_of, model_year, manufacturer, quantity)
  values(now(), 2010, 'Toyota', 12);

-- now make them all have the same as-of time - regardless
update cars
  set as_of = now();