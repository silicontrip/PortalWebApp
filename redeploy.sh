#!/bin/sh

/usr/local/glassfish4/bin/asadmin undeploy portalApi 
/usr/local/glassfish4/bin/asadmin deploy dist/portalApi.war
