# Fusion B — private original-weight research build

Primary route: **Rin Flanime Anima 70% + MiaoMiao Anima 30%**. This is a
candidate checkpoint, not an automatic quality improvement.

Sources are pinned by \`fusion_b_sources.json\` and checked at runtime:
- Rin Anima v1.4 original FP16, SHA256 \`a28d1e04787dcc58507feb7921e8fcbffc051dcf86d8685cf1049eedfc317be2\`
- MiaoMiao Harem Anima v1.6 original BF16/FP16 candidate (exact SHA pinned by preflight)
- Hassaku XL Illustrious v3.4, SHA256 \`1618edb443d9c641fb01c4961f5875d5f01b1a851fd9f2ea64623ceac82257e0\`, **excluded** from Anima tensor interpolation because its architecture differs.

**Important:** This Rin original is not proven identical to the user's installed
\`rin_flanime_v1_turbo\` QNN variant. The full merge needs actual inference and
qualitative sheet-fidelity benchmarks against Rin before it may be selected as
the automatic anime backbone.

## Cloud processing, no 64 GB computer
Hugging Face CPU Basic offers 16 GB RAM and 50 GB disk; the merge loops over
8 MiB tensor blocks and needs no CUDA. The full job downloads about 8–9 GB
of original weights and uploads about 4 GB to a **private** repository.

### Private storage requirement

Current connected HF OAuth credentials are *read/jobs only*. A private
model-repository write credential (via \`-s HF_TOKEN\`) is required to retain
the output. Jobs delete local data after finishing. **Do not start the
billable full merge without the write-capable private destination.**

Example after private repository and token authorization:

\`\`\`sh
hf jobs run --flavor cpu-basic --timeout 3h -s HF_TOKEN python:3.12-slim -- \
  bash -lc 'pip install -q numpy huggingface_hub hf_xet && \
  git clone --depth 1 https://github.com/Azrazieliz/AsterionCompiler.git /src && \
  python /src/fusion/run_private_fusion_b.py --private-repo Azraziel/Asterion-Art-Fusion-B'
\`\`\`

This job validates every source checksum and both safetensors graphs before
writing to a private repo. It does not attempt to merge QNN binary files.

### Following the merge
1. Render standardized tests from the original Rin and Fusion B weights.
2. User grades image quality/character identity/theme/element fidelity.
3. Adopt fused weights only if preferred and reliable.
4. Convert via an authorized QNN toolchain for Snapdragon, preserving original
   tensor and VAE compatibility.
5. Import the resulting 6–7 GB QNN ZIP through Asterion Art's native library.

The output should never be described as a tested Snapdragon model before
NPU conversion and on-device validation.
