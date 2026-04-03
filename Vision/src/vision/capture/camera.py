import logging
import sys

import cv2

log = logging.getLogger(__name__)


class Camera:
    def __init__(self, index: int):
        backend = cv2.CAP_DSHOW if sys.platform == "win32" else cv2.CAP_ANY
        self._cap = cv2.VideoCapture(index, backend)
        if not self._cap.isOpened():
            raise RuntimeError(f"Cannot open camera at index {index}")

    def read_frame(self):
        ok, frame = self._cap.read()
        if not ok:
            log.warning("Failed to read frame from camera")
            return None
        return frame

    def release(self):
        self._cap.release()
