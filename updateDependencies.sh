#!/bin/bash

./gradlew --refresh-dependencies dependencies spring-boot-tests:dependencies --update-locks '*:*'
