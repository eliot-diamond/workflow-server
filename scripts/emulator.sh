brokeruri="jms:jndi:dynamicQueues/GDA.QUEUE?replyToName=dynamicQueues/WF.QUEUE&jndiURL=tcp://localhost:61616&timeToLive=50000"

cd /scratch/astra/gphl-sdcp-emulation/com.globalphasing.sdcp.emulator.astra.gda

java -cp build/libs/GdaEmulator.jar:./resources/ \
    co.gphl.astra.gda.StartGdaServer \
    -cdir /scratch/astra/emulator-config \
    -brokeruri "$brokeruri" \
    -beamline i23 \
    -enactmentid $ASTRA_enactmentId \
    -wfconfig /dls_sw/i23/etc/astra/config \
    "$@"
    
