cd target
rm -f nlp-logs\*.log
rm -f *-shaded.jar
for nlpjar in nlp-service-*.jar; do 
  java -Ddw.server.applicationConnectors[0].port=8060 -Ddw.server.adminConnectors[0].port=8061 -Ddw.swagger.enabled=true -Dfile.encoding=UTF-8 -jar "$nlpjar" server nlp-default-config.yml
done
cd ..