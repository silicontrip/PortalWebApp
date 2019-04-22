#!/bin/sh

/Volumes/Octant/glassfish4/bin/asadmin undeploy portalApi 
/Volumes/Octant/glassfish4/bin/asadmin deploy dist/portalApi.war
