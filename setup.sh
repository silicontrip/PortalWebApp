#!/bin/sh

ASADMIN=/Users/mark/glassfish7/glassfish/bin/asadmin

$ASADMIN --passwordfile /dev/stdin create-file-user silicontrip <<EOF                                                                                                               │
AS_ADMIN_USERPASSWORD=test123
EOF 

$ASADMIN create-jms-resource --restype jakarta.jms.QueueConnectionFactory jms/QueueConnectionFactory

$ASADMIN create-jms-resource --restype jakarta.jms.Queue jms/portalQueue
$ASADMIN create-jms-resource --restype jakarta.jms.Queue jms/linkQueue
$ASADMIN create-jms-resource --restype jakarta.jms.Queue jms/cellQueue

$ASADMIN delete-jvm-options -Xmx512m
$ASADMIN create-jvm-options -Xmx4096m
