# Maria multi-provider gateway

The Android app now calls only `POST /api/chat`; provider keys remain on the gateway server. The gateway supports `auto`, `gemini`, `openai`, `claude`, and `openrouter`.

## Run locally

```bash
cd gateway
python3 -m venv .venv
. .venv/bin/activate
pip install -r requirements.txt
export OPENAI_API_KEY='...'
# or GEMINI_API_KEY, ANTHROPIC_API_KEY, OPENROUTER_API_KEY
python app.py
```

For an Android phone connecting to a computer on the same Wi-Fi, use the computer's LAN address, for example `http://192.168.1.20:8787`. For a phone outside the home network, deploy this gateway behind HTTPS; do not expose a development Flask server directly to the public internet.

In Maria Settings enter the gateway URL, optional gateway token, provider (`auto`, `gemini`, `openai`, `claude`, or `openrouter`), and model (`default` is accepted). Save, then use Test Connection.

A production deployment should add authentication, HTTPS, rate limiting, request logging with secrets redacted, and a real process manager. Never commit provider API keys to GitHub.
