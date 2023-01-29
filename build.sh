#!/bin/bash

mvn clean install package

cd elm
npm run build
cd ..
