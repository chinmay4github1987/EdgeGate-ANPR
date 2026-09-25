# Model assets

Put your trained detector here as **`plate_detector.tflite`** (see `ml/train_and_export.py`).

- If the file is present, the app runs the two-stage pipeline: LiteRT YOLOv8n plate
  detection (GPU delegate, CPU/XNNPACK fallback), then ML Kit OCR on the plate crop.
- If it is absent, the app still works: ML Kit OCR runs on the full frame and the
  plate grammar filter picks the plate out of the text. Slower and noisier, but zero setup.

Supported exports: float32, float16 or int8; input NHWC `[1,H,W,3]` (legacy `tflite` export) or NCHW `[1,3,H,W]` (Ultralytics `litert` export); output `[1, 5, N]` or `[1, N, 5]`.
