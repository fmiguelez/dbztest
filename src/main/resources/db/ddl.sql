ALTER USER postgres SET search_path to public; 

CREATE EXTENSION IF NOT EXISTS postgis;

DROP TABLE IF EXISTS public.test_geom;
CREATE TABLE public.test_geom(
id bigint NOT NULL,
CONSTRAINT test_geom_pk PRIMARY KEY (id)
);
SELECT AddGeometryColumn ('public','test_geom','geom',4326,'GEOMETRY',2);
