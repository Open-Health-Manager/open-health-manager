# Open Health Manager™

## About

Open Health Manager™ is the reference implementation for the MITRE Health Manager Lab under the Living Health Lab 
initiative. This project aims to radically flip the locus of health data from organizations
to individuals, promoting individual agency in personal health through primary self-care and 
engagement with their care team. 

A core component needed to effect that flip and enable individual action is a health manager that serves as the
repository for an individual's health data, collecting, combining, and making sense of health data as it is collected
and making it available for the individual to share. The Health Manager lab is working with the HL7™ community
to develop open standards for the collection, representation, and access of health data within a health manager.
Open Health Manager™ is a reference implementation of these standards and a demonstration platform for
what these standards and a patient-controlled health record can enable.

## Quickstart

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

To run the application using Docker:

```shell
cd [base directory]/open-health-manager-app/
./mvnw -Pprod -DskipTests verify jib:dockerBuild # Run this command to build the docker image of the application, -DskipTests is optional
docker-compose -f src/main/docker/app.yml -p open-health-manager up -d # Run this once the image is built to run the application using Docker
```

## FHIR Implementation Guides

Open Health Manager™ implements the following FHIR IGs
- [Patient Data Receipt](https://open-health-manager.github.io/patient-data-receipt-ig/) (under development)
- [Standard Patient Health Record](https://open-health-manager.github.io/standard-patient-health-record-ig/) (under development)

## Contributing

We love your input! We want to make contributing to this project as easy and transparent as possible, whether it's:

* Reporting a bug
* Discussing the current state of the code
* Submitting a fix
* Proposing new features
* Becoming a maintainer

### We Develop with GitHub

We use GitHub to host code, to track issues and feature requests, as well as accept pull requests.

### Report bugs using GitHub's issues

We use GitHub issues to track public bugs.

## License

Copyright 2022 The MITRE Corporation

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

```
http://www.apache.org/licenses/LICENSE-2.0
```

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.