import os
from pathlib import Path

import yaml


def load_config() -> dict:
    env = os.environ.get("VISION_ENV", "dev")
    config_path = Path(f"config-{env}.yaml")
    with open(config_path) as f:
        config = yaml.safe_load(f)
    _apply_env_overrides(config)
    return config


def _apply_env_overrides(config: dict) -> None:
    overrides = {
        "VISION_CAMERA_INDEX":          ("camera",    "index",                int),
        "VISION_WEBHOOK_HOST":          ("webhook",   "host",                 str),
        "VISION_WEBHOOK_PORT":          ("webhook",   "port",                 int),
        "VISION_WEBHOOK_PATH":          ("webhook",   "path",                 str),
        "VISION_CONFIDENCE_THRESHOLD":  ("inference", "confidence_threshold", float),
        "VISION_INTERVAL_SEC":          ("inference", "interval_sec",         float),
        "VISION_MODEL_WEIGHTS":         ("model",     "weights",              str),
    }
    for env_key, (section, key, cast) in overrides.items():
        val = os.environ.get(env_key)
        if val is not None:
            config[section][key] = cast(val)
