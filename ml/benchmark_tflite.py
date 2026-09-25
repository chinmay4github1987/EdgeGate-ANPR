"""
Compare exported model variants on the desktop before touching a phone.

    python benchmark_tflite.py exports/*.tflite --runs 100

Prints size, input/output dtypes and latency percentiles for each file.
Desktop numbers are NOT phone numbers – use them to rank variants, then confirm
on-device with the app's live HUD or Google's `benchmark_model` tool:

    adb push plate_detector.tflite /data/local/tmp/
    adb shell /data/local/tmp/benchmark_model --graph=/data/local/tmp/plate_detector.tflite \
        --use_gpu=true --num_runs=100
"""
import argparse
import time
from pathlib import Path

import numpy as np

try:
    from ai_edge_litert.interpreter import Interpreter  # pip install ai-edge-litert
except ImportError:  # older environments
    from tensorflow.lite.python.interpreter import Interpreter


def bench(path: Path, runs: int, threads: int) -> None:
    it = Interpreter(model_path=str(path), num_threads=threads)
    it.allocate_tensors()
    inp = it.get_input_details()[0]
    out = it.get_output_details()[0]

    if inp["dtype"] == np.float32:
        x = np.random.rand(*inp["shape"]).astype(np.float32)
    else:
        info = np.iinfo(inp["dtype"])
        x = np.random.randint(info.min, info.max + 1, size=inp["shape"], dtype=inp["dtype"])

    for _ in range(10):  # warm-up: first runs include allocation and cache effects
        it.set_tensor(inp["index"], x)
        it.invoke()

    times = []
    for _ in range(runs):
        t0 = time.perf_counter()
        it.set_tensor(inp["index"], x)
        it.invoke()
        _ = it.get_tensor(out["index"])
        times.append((time.perf_counter() - t0) * 1000)

    t = np.array(times)
    print(
        f"{path.name:40s} {path.stat().st_size / 1e6:6.2f} MB  in={np.dtype(inp['dtype']).name:7s} "
        f"out={np.dtype(out['dtype']).name:7s} shape_out={tuple(out['shape'])}  "
        f"p50={np.percentile(t, 50):6.1f} ms  p90={np.percentile(t, 90):6.1f} ms"
    )


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("models", nargs="+", type=Path)
    ap.add_argument("--runs", type=int, default=100)
    ap.add_argument("--threads", type=int, default=4)
    args = ap.parse_args()
    for m in args.models:
        bench(m, args.runs, args.threads)


if __name__ == "__main__":
    main()
