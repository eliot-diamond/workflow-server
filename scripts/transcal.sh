#!/bin/bash

. `dirname $0`/astra

workflow="/scratch/gda_development/i23-build/workspace_git/gda-mx.git/plugins/com.globalphasing.astra-wf-devel/lib/*"

# CLASSPATH=$workflow_jar

lib="/scratch/gda_development/i23-build/astra_git/*"

# CLASSPATH=$CLASSPATH:$lib_dir/com.globalphasing.sdcp.common.jar
# CLASSPATH=$CLASSPATH:$lib_dir/com.globalphasing.sdcp.abstract_beamline.jar
# CLASSPATH=$CLASSPATH:$lib_dir/com.globalphasing.astra.messages.jar
# CLASSPATH=$CLASSPATH:$lib_dir/com.globalphasing.astra.messagebus.jar
# CLASSPATH=$CLASSPATH:$lib_dir/com.globalphasing.gcal.aux.jar

CLASSPATH=$lib:$workflow

external_libs=/scratch/gda_development/i23-build/workspace/tp/plugins
CLASSPATH=$CLASSPATH:${external_libs}/log4j.over.slf4j_1.7.25.jar
CLASSPATH=$CLASSPATH:${external_libs}/slf4j.api_1.7.25.jar
CLASSPATH=$CLASSPATH:${external_libs}/javax.jms_1.1.0.v201205091237.jar
CLASSPATH=$CLASSPATH:${external_libs}/org.apache.activemq.activemq-osgi_5.15.6.jar
CLASSPATH=$CLASSPATH:${external_libs}/javax.management.j2ee-api_1.1.2.jar

echo "CLASSPATH=$CLASSPATH"

java -version

mkdir -p $ASTRA_wdir

##### DO NOT CHECK IN ####################################
# debug configuration directory
#config_dir=/dls_sw/i23/etc/astra/config
config_dir=/scratch/astra/config

# Over write the enactment ID 
# ASTRA_enactmentId=0baf878f-903b-4802-9587-3c7961ab675b
##########################################################

echo "******************* Started transcal workflow $ASTRA_enactmentId *********************************"

java \
-Dorg.apache.activemq.SERIALIZABLE_PACKAGES=* \
-Dco.gphl.wf.logging.level=ALL \
-Dco.gphl.wf.use_acknowledge=F \
-Dfile.encoding=UTF-8 \
-Djava.util.logging.SimpleFormatter.format='%2$s %5$s%6$s%n' \
-Dco.gphl.astra.msgdir=/scratch/astra \
-classpath $CLASSPATH \
co.gphl.wf.workflows.WFTransCal \
-beamline "jms:jndi:dynamicQueues/WF.QUEUE?replyToName=dynamicQueues/GDA.QUEUE&jndiURL=$BROKER_URI&enactmentId=$ASTRA_enactmentId&timeToLive=5000" \
-wdir $ASTRA_wdir \
-persistname astra \
-wfprefix transcal \
-calibration TransCal \
-propertyfile $config_dir/workflow.properties \
-file $config_dir/transcal_test_10deg.dat \
-updateblconfig

echo "******************* Finished transcal workflow $ASTRA_enactmentId *********************************"

#-grid \
#omega:0:0:0 \
#phi:0:360:45 \
#kappa:-190:10:45