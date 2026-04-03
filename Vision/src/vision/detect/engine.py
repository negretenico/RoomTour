import logging

from ultralytics import YOLO

log = logging.getLogger(__name__)


class InferenceEngine:
    def __init__(self, weights: str, confidence_threshold: float):
        self._model = YOLO(weights)
        self._threshold = confidence_threshold

    def detect(self, frame) -> list[dict]:
        results = self._model(frame, verbose=False)
        detected = []
        for result in results:
            for box in result.boxes:
                conf = float(box.conf[0])
                if conf >= self._threshold:
                    label = result.names[int(box.cls[0])]
                    detected.append({"label": label, "confidence": conf})
        return detected
