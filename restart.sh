#!/bin/bash

# mvn test

# mvn verify -Pintegration-tests

mvn clean package -DskipTests && docker-compose down && docker-compose build && docker-compose up -d

# mvn clean package -DskipTests && docker-compose up -d --build           # если код изменился, но Dockerfile и docker-compose.yml остались прежними