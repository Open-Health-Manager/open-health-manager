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

### running
```shell
cd [base directory]/open-health-manager-app/
mvn
```

## License

Copyright 2022 The MITRE Corporation

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

```
http://www.apache.org/licenses/LICENSE-2.0
```

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.