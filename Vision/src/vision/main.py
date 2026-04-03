import logging
import queue
import threading
import time

from vision.capture.camera import Camera
from vision.config.loader import load_config
from vision.detect.engine import InferenceEngine
from vision.push.webhook import push

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s — %(message)s",
)
log = logging.getLogger(__name__)

_frame_queue: queue.Queue = queue.Queue(maxsize=1)


def _worker(engine: InferenceEngine, webhook_url: str) -> None:
    while True:
        frame = _frame_queue.get()
        if frame is None:
            break
        detected = engine.detect(frame)
        log.info("Detected %d object(s): %s", len(detected), detected)
        push(detected, webhook_url)
        _frame_queue.task_done()


def main() -> None:
    config = load_config()

    webhook_url = (
        f"http://{config['webhook']['host']}"
        f":{config['webhook']['port']}"
        f"{config['webhook']['path']}"
    )

    engine = InferenceEngine(
        config["model"]["weights"],
        config["inference"]["confidence_threshold"],
    )
    camera = Camera(config["camera"]["index"])
    interval = config["inference"]["interval_sec"]

    worker_thread = threading.Thread(
        target=_worker, args=(engine, webhook_url), daemon=True
    )
    worker_thread.start()

    log.info("Vision loop started — webhook=%s interval=%.1fs", webhook_url, interval)

    try:
        while True:
            frame = camera.read_frame()
            if frame is not None:
                log.info("Frame captured — queuing for inference")
                # Sliding window: drop the oldest frame if the worker hasn't caught up
                if _frame_queue.full():
                    try:
                        _frame_queue.get_nowait()
                    except queue.Empty:
                        pass
                _frame_queue.put(frame)
            time.sleep(interval)
    except KeyboardInterrupt:
        log.info("Shutting down")
    finally:
        _frame_queue.put(None)
        worker_thread.join()
        camera.release()


if __name__ == "__main__":
    main()
