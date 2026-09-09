import os
from flask import Flask, jsonify, request
import requests

app = Flask(__name__)


def chat_openai_compatible(provider, message, model):
    configs = {
        "openai": ("https://api.openai.com/v1/chat/completions", os.getenv("OPENAI_API_KEY"), model or "gpt-4o-mini"),
        "gemini": ("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions", os.getenv("GEMINI_API_KEY"), model or "gemini-2.5-flash"),
        "openrouter": ("https://openrouter.ai/api/v1/chat/completions", os.getenv("OPENROUTER_API_KEY"), model or "openai/gpt-4o-mini"),
    }
    url, key, chosen_model = configs[provider]
    if not key:
        raise RuntimeError(f"{provider} provider key is not configured on the gateway")
    response = requests.post(url, headers={"Authorization": f"Bearer {key}", "Content-Type": "application/json"}, json={"model": chosen_model, "messages": [{"role": "user", "content": message}]}, timeout=60)
    response.raise_for_status()
    return response.json()["choices"][0]["message"]["content"]


def chat_claude(message, model):
    key = os.getenv("ANTHROPIC_API_KEY")
    if not key:
        raise RuntimeError("claude provider key is not configured on the gateway")
    response = requests.post("https://api.anthropic.com/v1/messages", headers={"x-api-key": key, "anthropic-version": "2023-06-01", "content-type": "application/json"}, json={"model": model or "claude-3-5-haiku-latest", "max_tokens": 1024, "messages": [{"role": "user", "content": message}]}, timeout=60)
    response.raise_for_status()
    return response.json()["content"][0]["text"]


def choose_provider(requested):
    if requested != "auto":
        return requested
    for name, variable in (("openai", "OPENAI_API_KEY"), ("gemini", "GEMINI_API_KEY"), ("claude", "ANTHROPIC_API_KEY"), ("openrouter", "OPENROUTER_API_KEY")):
        if os.getenv(variable):
            return name
    raise RuntimeError("No provider key is configured on the gateway")


@app.get("/health")
def health():
    return jsonify({"ok": True, "service": "maria-gateway"})


@app.post("/api/chat")
def chat():
    try:
        body = request.get_json(force=True) or {}
        message = str(body.get("message", "")).strip()
        if not message:
            return jsonify(error="message is required"), 400
        provider = choose_provider(str(body.get("provider", "auto")).lower())
        model = str(body.get("model", "")).strip()
        text = chat_claude(message, model) if provider == "claude" else chat_openai_compatible(provider, message, model)
        return jsonify(text=text, provider=provider, model=model or "default")
    except requests.HTTPError as exc:
        detail = exc.response.text[:400] if exc.response is not None else str(exc)
        return jsonify(error=f"Provider HTTP error: {detail}"), 502
    except Exception as exc:
        return jsonify(error=str(exc)), 400


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(os.getenv("PORT", "8787")))
