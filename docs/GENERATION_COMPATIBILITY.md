# Generation compatibility backend

The Asterion Art APK can submit genuine five-pass image jobs using the Local Dream native QNN host over device loopback. **This is not yet an embedded native engine**. The Android native QNN runtime, model fusion, reliable multimodal reference editing, video engine and proprietary SuperImage Pro are outstanding integrations.

Workflow:
1. Activate the installed Local Dream host on the phone. Anima img2img VAE encoder must be present for passes 2–5.
2. Choose completed character, style and theme sheets (optional elements). Text extraction reads actual source metadata via ML Kit OCR; it never invents depicted identities.
3. Runtime selects preferred compatible installed model automatically and verifies all five prompts against its tokenizer.
4. Pass 1 is text-to-image; passes 2–5 use previous PNG as img2img input with controlled denoise. WorkManager persists intermediate PNGs and resumes completed stages after interruption.
5. Final candidates receive a technical-only score for contrast and detail and are saved with SHA256/seed/model provenance to Pictures/AsterionArt/Generated. Users grade results manually.
6. Job cancellation, thermal guard and a foreground notification are available. Android can still limit background runtime.

Visual sheet fidelity and consistency are **not** measured yet. OCR of complex boards is fallible; jobs fail rather than silently hallucinating when authoritative textual evidence is unavailable or exceeds model context. A locally started host may terminate when its application is stopped; native single-app embedding is required for truly standalone execution.
