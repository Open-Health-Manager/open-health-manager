# open-health-manager

## Building and running

### check out and build
```shell
git clone --recurse-submodules git@github.com:Open-Health-Manager/open-health-manager.git
cd open-health-manager/hapi-fhir-jpaserver-starter
mvn install -DskipTests
mvn install:install-file -Dfile=target/ROOT-classes.jar -DgroupId=ca.uhn.hapi.fhir -DartifactId=hapi-fhir-jpaserver-starter -Dversion=5.7.0 -Dpackaging=jar
cd ..
mvn install -DskipTests
```
