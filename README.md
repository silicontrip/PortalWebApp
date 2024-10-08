# Setup

After installing glassfish 7

## start derby database

from the glassfish install directory. Specify where javadb is installed.

`./glassfish/bin/asadmin start-database --dbhome=/Users/silicontrip/glassfish7/javadb`


## create JDBC resource:

On the glassfish admin page 

Resources/JDBC/JDBC Resources

New

JNDI name: jdbc/IngressResource

the default JDBC connection pool "DerbyPool" uses sun-appserv-samples as the database name l/p APP/APP


## create schema tables:

using the derby 'ij' command line tool

depending on where java is installed

`export JAVA_HOME=/usr/local/Cellar/openjdk/21.0.1`

from the glassfish install directory

`./javadb/bin/ij`

```
DRIVER 'org.apache.derby.jdbc.ClientDriver';
CONNECT 'jdbc:derby://localhost:1527/sun-appserv-samples' USER 'APP' PASSWORD 'APP';
```


### MUCELL
`create table mucell ( cellid varchar(10) not null default '', mu_low double default null, mu_high double default null, primary key (cellid) );`

### MUFIELDS
`create table mufields (creator varchar(36) default null, agent varchar(36) not null, mu int not null, guid char(36) not null primary key, timestamp varchar(24) default null, team char(1) default null, pguid1 char(36) default null, plat1 int not null, plng1 int not null, pguid2 char(36) default null, plat2 int not null, plng2 int not null, pguid3 char(36) default null, plat3 int not null, plng3 int not null, valid boolean default null );`

I'm not sure about these key indexes
key mufield_plat1 (plat1), key mufield_plat2 (plat2), key mufield_plat3 (plat3), key mufield_plng1 (plng1), key mufield_plng2 (plng2), key mufield_plng3 (plng3) );

```
create index mufield_plat1 on mufields (plat1);
create index mufield_plng1 on mufields (plng1);
create index mufield_plat2 on mufields (plat2);
create index mufield_plng2 on mufields (plng2);
create index mufield_plat3 on mufields (plat3);
create index mufield_plng3 on mufields (plng3);
```

### PORTALS
`create table portals ( guid varchar(36) not null primary key, title varchar(256), latE6 int not null, lngE6 int not null, team varchar(16) not null, level smallint default null, res_count smallint default null, health smallint default null, time_lastseen int default null, deleted boolean default false, image varchar(160) default null);`

### LINKS
`create table links (guid varchar(36) not null primary key, d_guid varchar(36) not null, d_latE6 int not null, d_lngE6 int not null, o_guid varchar(36) not null, o_latE6 int not null, o_lngE6 int not null, team varchar(16));`

### FIELDCELLS
```
create table fieldcells (field_guid varchar(36) not null, cellid bigint default null, unique(field_guid,cellid));
create index fieldcells_cellid on fieldcells (cellid);
```


## Start glassfish domain

`./glassfish/bin/asadmin start-domain`

To connect remotely (not needed if setting up as localhost)
`./glassfish/bin/asadmin change-admin-password`

`./glassfish/bin/asadmin enable-secure-admin`

## create users 

in Configurations/server-config/Security/Realms/file Manage Users

`User ID: `

`password (apikey passed in via post or get)`

for accessing the webapp pages

## create JMS Connection Factory:

`JNDI Name: jms/QueueConnectionFactory`

`Resource type: jakata.jms.QueueConnectionFactory`

### create JMS Queue: 

`JNDI name: jms/portalQueue`

`physical destination name: portalQueue`

`Resource Type: jakarta.jms.Queue`

repeat as above for these queues

create JMS Queue: jms/linkQueue

create JMS Queue: jms/fieldQueue

## start the web domain

from the glassfish install directory

`./glassfish/bin/asadmin start-domain`

## deploy web app

Point to the path where the portalApi.war has been built

`./glassfish/bin/asadmin deploy dist/portalApi.war`

## undeploy the web app (when upgrading)

`./glassfish/bin/asadmin undeploy portalApi`
