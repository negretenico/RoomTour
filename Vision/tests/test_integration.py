"""
Integration test: real HTTP webhook receiver + real YOLO inference + real HTTP POST.
Camera is replaced with a synthetic black frame (no hardware needed).
"""
import json
import threading
from http.server import BaseHTTPRequestHandler, HTTPServer
from unittest.mock import patch

import numpy as np
import pytest

from vision.detect.engine import InferenceEngine
from vision.push.webhook import push


class _WebhookCapture(BaseHTTPRequestHandler):
    """Minimal HTTP server that records the last POST body."""

    received: list[dict] = []

    def do_POST(self):  # noqa: N802
        length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(length)
        _WebhookCapture.received.append(json.loads(body))
        self.send_response(200)
        self.end_headers()

    def log_message(self, *_):  # silence access log during tests
        pass


@pytest.fixture(scope="module")
def webhook_server():
    _WebhookCapture.received.clear()
    server = HTTPServer(("localhost", 0), _WebhookCapture)
    port = server.server_address[1]
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    yield f"http://localhost:{port}/webhook"
    server.shutdown()


@pytest.fixture(scope="module")
def engine():
    return InferenceEngine(weights="yolov8n.pt", confidence_threshold=0.0)


def synthetic_frame():
    """240×320 black BGR frame — valid input for YOLO, produces zero or more detections."""
    return np.zeros((240, 320, 3), dtype=np.uint8)


# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------

def test_detect_returns_list_of_dicts(engine):
    detections = engine.detect(synthetic_frame())
    assert isinstance(detections, list)
    for d in detections:
        assert "label" in d
        assert "confidence" in d
        assert isinstance(d["confidence"], float)


def test_push_over_real_http(webhook_server):
    _WebhookCapture.received.clear()
    detections = [{"label": "chair", "confidence": 0.91}]
    push(detections, webhook_server)
    assert len(_WebhookCapture.received) == 1
    payload = _WebhookCapture.received[0]
    assert payload == {"detectedObjects": detections}


def test_full_detect_then_push_pipeline(engine, webhook_server):
    """Detect on a synthetic frame, then push the result over real HTTP."""
    _WebhookCapture.received.clear()
    detections = engine.detect(synthetic_frame())
    push(detections, webhook_server)
    assert len(_WebhookCapture.received) == 1
    payload = _WebhookCapture.received[0]
    assert "detectedObjects" in payload
    assert isinstance(payload["detectedObjects"], list)
