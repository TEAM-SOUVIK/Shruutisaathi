# Legacy web prototype

This Flask/Whisper/BART/gTTS experiment is retained for historical comparison.
It is not part of the canonical Android runtime and is not production-ready.

Important limitations include online gTTS, download-prone model dependencies,
unversioned Python packages, fixed output names, Flask debug mode, and missing
upload, concurrency, privacy, and security controls.

If historical reproduction is required, run commands from this directory so
the local template and generated files remain isolated:

```powershell
Set-Location 'D:\Empower26\CodeX\AI-hearing-systems\legacy\web-prototype'
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python app.py
```

This procedure requires substantial model downloads, FFmpeg, and network access.
Do not run it as part of the Android baseline.
