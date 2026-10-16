# Ruhi TensorFlow intent model, version 2

Ruhi now also has a separate contextual dialogue head and deterministic pending-command manager. See [conversation architecture](RUHI_CONVERSATION_ARCHITECTURE.md) and [dialogue evidence](ruhi-dialogue-evidence/REPORT.md). The intent and engine-selection models described below retain their existing weights.

The app bundles a TensorFlow 2.20-trained linear softmax classifier with 26 intent labels. LiteRT 2.1.0 performs real on-device CPU inference. `ruhi_nlp/model.tflite` and `graph_intent.tflite` contain identical model bytes; vocabulary, labels and SHA-256 are stored in metadata.json.

Training uses 4501 unique authored examples, including the 89 developer-supplied commands and numeric/object/polite variants. Classes are balanced during optimization. No harvested student conversations, correction records, network inference or runtime retraining are used.

Inputs are binary ASCII-English unigram/bigram features; numeric tokens become `num`. The model predicts the action category. Deterministic parsers extract and validate entity references, coordinates, dimensions and operation parameters. Models never generate mathematical values, formulas, meshes or pixels. This model is not unrestricted NLU or a learned entity extractor.

The requested examples score 89/89 for intent recognition; this is training fit. The 36 development paraphrases score 36/36, but were inspected during development and are not an independent benchmark. A separate frozen set of 26 unseen complete sentences scores 26/26; 23 meet the 0.85 confidence / 0.35 margin gate, with zero accepted errors on that small authored set. Related words/templates remain in training, so none of these numbers establishes unrestricted English, multilingual or microphone-recognition accuracy.

Actual Android inference over all 89 examples and the existing paraphrase-to-command execution check pass (two instrumentation methods). The 61 JVM regressions pass. Engine capability is separate: unsupported polyhedra/constructions remain unsupported, invalid numeric values remain rejected, and missing distances/angles/scales require clarification. The UI and lessons are unchanged.

The separate engine-selection model is unchanged and retains its earlier 83.6% authored benchmark. Its score must not be conflated with the intent model results here.

Reproduce with `scripts/ruhi-nlp-requirements.txt` on Python 3.12: run `scripts/train_ruhi_graph_nlp.py`, `scripts/evaluate_ruhi_requested_nlp.py`, then `scripts/build_ruhi_nlp_training_report.py`. Full examples, confidence values, source/model hashes and logs are in [the training report](ruhi-nlp-command-training/REPORT.md).

Current intent model SHA-256: `782fa1398227e8dde6c2b5d6a1e0af3d3b2ba07641400a49511316af4ef55ffa`.
