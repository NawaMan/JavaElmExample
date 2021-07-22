#!/bin/bash

mvn clean install package -DskipTest
java -jar target/JavaElmExample.jar
