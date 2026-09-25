"""
Train a YOLO number-plate detector and export it for on-device use (LiteRT / TFLite).

    pip install -r requirements.txt
    python train_and_export.py --data plates.yaml --epochs 80 --imgsz 320

Output: exports/plate_detector_int8.tflite  (copy to app/src/main/assets/plate_detector.tflite)

Why these choices (Edge AI trade-offs):
  * A "nano" model (YOLOv8n / YOLO11n / YOLO26n, ~2-3M params) is fast enough for real-time
    use on mid-range phones.
  * imgsz 320 instead of 640: 4x fewer pixels -> ~4x less compute. At a gate the car is
    close, so the plate is still large enough at 320 px. Measure recall before going smaller.
  * INT8 static quantisation: ~4x smaller than FP32 and faster on CPU/NPU, usually a small
    mAP loss. It needs a representative calibration set (the `data` yaml).
  * An FP32 export is also written for comparison; the GPU delegate runs FP16 internally.

Export API note: Ultralytics 8.4.83+ replaced format="tflite" with format="litert"
(quantize=8 for static INT8; input layout NCHW). Older versions only have "tflite"
(int8=True; input layout NHWC). This script tries the new API first and falls back.
The Android detector reads the layout from the model, so either file works.
"""
import argparse
import shutil
from pathlib import Path

from ultralytics import YOLO


def export_variant(model: YOLO, imgsz: int, data: str, int8: bool) -> Path:
    try:  # Ultralytics >= 8.4.83
        kwargs = {"quantize": 8, "data": data} if int8 else {}
        return Path(model.export(format="litert", imgsz=imgsz, **kwargs))
    except (ValueError, KeyError, TypeError, NotImplementedError) as e:  # older versions
        print(f"'litert' export unavailable ({e}); using legacy 'tflite' export")
        kwargs = {"int8": True, "data": data} if int8 else {}
        return Path(model.export(format="tflite", imgsz=imgsz, **kwargs))


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--data", default="plates.yaml", help="YOLO dataset yaml (single class: plate)")
    ap.add_argument("--epochs", type=int, default=80)
    ap.add_argument("--imgsz", type=int, default=320)
    ap.add_argument("--batch", type=int, default=32)
    ap.add_argument("--weights", default="yolov8n.pt", help="COCO-pretrained nano weights to start from")
    args = ap.parse_args()

    model = YOLO(args.weights)
    model.train(
        data=args.data,
        epochs=args.epochs,
        imgsz=args.imgsz,
        batch=args.batch,
        # Augmentations that match gate conditions: night glare, slight tilt.
        hsv_v=0.5, degrees=5.0, perspective=0.0005,
        fliplr=0.0,  # never flip horizontally - mirrored text teaches the wrong thing
        patience=20,
    )
    metrics = model.val(data=args.data, imgsz=args.imgsz)
    print(f"val mAP50={metrics.box.map50:.3f}  mAP50-95={metrics.box.map:.3f}")

    out = Path("exports")
    out.mkdir(exist_ok=True)
    for int8, suffix in ((False, "fp32"), (True, "int8")):
        src = export_variant(model, args.imgsz, args.data, int8)
        if src.is_dir():  # some versions return the export directory
            candidates = sorted(src.glob("*int8*.tflite" if int8 else "*float32*.tflite")) or sorted(src.glob("*.tflite"))
            src = candidates[0]
        dst = out / f"plate_detector_{suffix}.tflite"
        shutil.copy(src, dst)
        print(f"{suffix}: {dst}  ({dst.stat().st_size / 1e6:.2f} MB)")

    print("Copy the variant you choose to app/src/main/assets/plate_detector.tflite")


if __name__ == "__main__":
    main()
