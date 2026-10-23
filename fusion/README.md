# Experimental anime model fusion

A stream-bounded tensor interpolation utility for **original compatible Anima safetensors**, not converted QNN packages. Rin Flanime and MiaoMiao are the intended candidates, subject to confirming the original source checkpoint licence and tensor compatibility. Hassaku XL Illustrious is an SDXL-family checkpoint and must **not** be averaged into Anima tensors. Use separate training/distillation research for cross-family transfer.

Install numpy and run `python3 fusion/merge_anima.py --rin rin.safetensors --miaomiao miaomiao.safetensors --output fused.safetensors --miaomiao-weight .3` on a suitable private machine or a remote Hugging Face Job. This does not upload weights to ChatGPT or consume the user's PC RAM; source checkpoints must be accessible to the executor.

No model was downloaded, merged, validated for image quality, converted to NPU, or released by this commit. Running a cloud job may incur cost; avoid doing so without explicit authorization. Test quality against untouched Rin before using a merged candidate.
