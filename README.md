# JMetaAgents [![Build Status](https://travis-ci.org/welle/JUnitGenerator.svg?branch=master)](https://travis-ci.org/welle/JUnitGenerator) [![Quality Gate](https://sonarcloud.io/api/badges/gate?key=aka.junitgenerator:JUnitGenerator)](https://sonarcloud.io/dashboard/index/aka.junitgenerator:JUnitGenerator) #

Quick & dirty code to create JUnit tests skeletons for classes (can be in a jar).
Classes with dependencies will not be used and no test unit will be generated.

See Test unit a short example...

### Version

Go to [my maven repository](https://github.com/welle/maven-repository) to get the latest version.

## Notes
Need the eclipse-external-annotations-m2e-plugin: 

p2 update site to install this from: http://www.lastnpe.org/eclipse-external-annotations-m2e-plugin-p2-site/ (The 404 is normal, just because there is no index.html; it will work in Eclipse.)