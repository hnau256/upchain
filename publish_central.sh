#!/bin/bash
set -e

./gradlew publishToMavenCentral --no-configuration-cache -PsignAllPublications=true