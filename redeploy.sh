#!/bin/sh

/Users/mark/Developer/ingress-j2ee/glassfish4/bin/asadmin undeploy portalApi 
/Users/mark/Developer/ingress-j2ee/glassfish4/bin/asadmin deploy dist/portalApi.war
