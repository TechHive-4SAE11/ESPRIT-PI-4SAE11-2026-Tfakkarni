#!/bin/bash
# Verify the API gateway and at least 3 core services are up
curl -sf http://localhost:9090/actuator/health | grep -q '"status":"UP"' && \
curl -sf http://localhost:18081/actuator/health | grep -q '"status":"UP"' && \
curl -sf http://localhost:18082/actuator/health | grep -q '"status":"UP"' && \
curl -sf http://localhost:18086/actuator/health | grep -q '"status":"UP"'
