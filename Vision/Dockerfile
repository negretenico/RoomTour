# Phase 3+: swap for nvcr.io/nvidia/l4t-pytorch for Jetson GPU acceleration
FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY src/ src/
COPY config-dev.yaml config-dev.yaml
COPY config-prod.yaml config-prod.yaml

ENV VISION_ENV=prod
ENV PYTHONPATH=/app/src

CMD ["python", "-m", "vision.main"]
