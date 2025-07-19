#!/bin/bash
mvn test
mvn verify -Pintegration-tests
mvn clean package -DskipTests \
  && docker-compose down \
  && docker-compose build \
  && docker-compose up -d