cd target
del nlp-logs\*.log
del *-shaded.jar
for %%i in (nlp-service-*.jar) do java -Ddw.server.applicationConnectors[0].port=8060 -Ddw.server.adminConnectors[0].port=8061 -Ddw.swagger.enabled=true -Dfile.encoding=UTF-8 -jar %%i server nlp-default-config.yml
cd ..