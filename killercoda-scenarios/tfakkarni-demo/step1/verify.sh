#!/bin/bash
# Verify discovery-service and config-service are healthy
curl -sf http://localhost:8761/actuator/health | grep -q '"status":"UP"' && \
curl -sf http://localhost:8888/actuator/health | grep -q '"status":"UP"'
