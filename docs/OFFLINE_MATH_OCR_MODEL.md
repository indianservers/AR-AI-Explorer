# Offline Image-to-LaTeX model

AI Explorer Smart Board uses an optional quantized TexTeller ONNX model for dedicated
mathematical image recognition. It is separate from generic ML Kit text OCR.

## Runtime behavior

- The model is installed explicitly from Smart Board settings.
- The approximately 244 MiB pack is stored in private application storage.
- Downloads are resumable and every artifact is checked against its expected byte size and
  SHA-256 digest before activation.
- After installation, recognition runs fully offline through ONNX Runtime for Android.
- If the pack is unavailable or cannot execute, the existing local ML Kit image recognizer is
  used as a disclosed fallback; source ink is never replaced automatically.
- The dedicated result is fused with digital ink, parser verification, board context and
  correction personalization.

## Model and license

- Model: [onnx-community/TexTeller-ONNX](https://huggingface.co/onnx-community/TexTeller-ONNX)
- Upstream model: [OleehyO/TexTeller](https://huggingface.co/OleehyO/TexTeller)
- License: Apache License 2.0
- Runtime: [ONNX Runtime for Android](https://onnxruntime.ai/docs/install/)

The model pack is not included in the base APK. Users choose whether to download it. Removing the
pack deletes all downloaded weights and vocabulary data from the application’s private storage.
