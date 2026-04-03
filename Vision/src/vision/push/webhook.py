import logging

import requests

log = logging.getLogger(__name__)


def push(detected_objects: list[dict], webhook_url: str) -> None:
    payload = {"detectedObjects": detected_objects}
    try:
        r = requests.post(webhook_url, json=payload, timeout=5)
        log.info("Webhook POST → %s %d", webhook_url,r.status_code)
    except requests.RequestException as e:
        log.warning("Webhook push failed — dropping frame: %s", e)
