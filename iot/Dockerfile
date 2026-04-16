FROM python:3.12-slim

WORKDIR /app

RUN pip install --no-cache-dir requests pyserial

COPY iot/iot_steamer.py .

# In Docker there is no serial port — force full mock mode via env vars
ENV MOCK_GPS=true
ENV MOCK_BPM=true

ENTRYPOINT ["python", "-u", "iot_steamer.py"]
