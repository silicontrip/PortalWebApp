/Volumes/Octant/glassfish4/bin/asadmin disable portalApi
/Volumes/Octant/glassfish4/bin/asadmin flush-jmsdest --desttype queue CellQueue
/Volumes/Octant/glassfish4/bin/asadmin flush-jmsdest --desttype queue FieldQueue
/Volumes/Octant/glassfish4/bin/asadmin flush-jmsdest --desttype queue LinkQueue
/Volumes/Octant/glassfish4/bin/asadmin flush-jmsdest --desttype queue PortalQueue
/Volumes/Octant/glassfish4/bin/asadmin enable portalApi
