import requests as req
from unittest.mock import patch

from vision.push.webhook import push

WEBHOOK_URL = "http://localhost:8080/api/v1/recognition/classify"


def test_push_sends_correct_json_shape():
    detected = [{"label": "chair", "confidence": 0.85}]
    with patch("vision.push.webhook.requests.post") as mock_post:
        push(detected, WEBHOOK_URL)
        mock_post.assert_called_once_with(
            WEBHOOK_URL,
            json={"detectedObjects": detected},
            timeout=5,
        )


def test_push_sends_multiple_detections():
    detected = [
        {"label": "chair", "confidence": 0.85},
        {"label": "sofa", "confidence": 0.72},
    ]
    with patch("vision.push.webhook.requests.post") as mock_post:
        push(detected, WEBHOOK_URL)
        mock_post.assert_called_once_with(
            WEBHOOK_URL,
            json={"detectedObjects": detected},
            timeout=5,
        )


def test_push_sends_empty_detections():
    with patch("vision.push.webhook.requests.post") as mock_post:
        push([], WEBHOOK_URL)
        mock_post.assert_called_once_with(
            WEBHOOK_URL,
            json={"detectedObjects": []},
            timeout=5,
        )


def test_push_does_not_raise_on_connection_error():
    with patch("vision.push.webhook.requests.post", side_effect=req.ConnectionError("refused")):
        push([{"label": "stove", "confidence": 0.91}], WEBHOOK_URL)


def test_push_does_not_raise_on_timeout():
    with patch("vision.push.webhook.requests.post", side_effect=req.Timeout("timed out")):
        push([{"label": "sink", "confidence": 0.66}], WEBHOOK_URL)
