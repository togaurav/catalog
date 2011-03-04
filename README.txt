Setting up CatalogService

Requirements 
1) MySQL DB - configuration is in src/main/resources/configuration.properties
2) Jetty instance to deploy created war, you may have to configure ${JETTY_HOME}/etc/jetty.xml to allow war deployment, or use a context.xml file to point to war file
3) Latest Shared Libs

Build
1) run "ant war" from the Catalog folder, the target will create the war file in target/dist/war/catalog-service.war
or
2) mvn clean package
Running JUnit Integration tests in Eclipse
Should be as simple as selecting the test and right click/option click and select run as junit