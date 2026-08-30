#!/usr/bin/env python3
"""Bake the kiosk's fixed robot-voice lines with ElevenLabs into app assets.

Uses the same key + Turkish voice as Rozy Assistant (read from its
local.properties; override with ELEVENLABS_API_KEY / ELEVENLABS_VOICE_ID env vars).
Model follows Rozy Assistant's prebaked convention: eleven_multilingual_v2.

Run:  python3 scripts/generate_voice.py
"""
import json
import os
import pathlib
import subprocess
import sys

HERE = pathlib.Path(__file__).resolve().parent
ASSETS = HERE.parent / "app" / "src" / "main" / "assets" / "voice"
ROZY_PROPS = HERE.parent.parent.parent / "Rozy_Assistant" / "local.properties"

LINES = {
    # Idle invite — plays on the welcome screen.
    "invite": "Bugünkü şansına merhaba demek ister misin? Ekrana dokun ve oyuna başla!",
    # Reveal — plays when the card flips. Amount-agnostic so any promo config works.
    "win": "Tebrikler! Avantajına merhaba!",
}
MODEL_ID = "eleven_multilingual_v2"


def props(path):
    out = {}
    if path.exists():
        for line in path.read_text().splitlines():
            if "=" in line and not line.lstrip().startswith("#"):
                k, v = line.split("=", 1)
                out[k.strip()] = v.strip()
    return out


def main():
    p = props(ROZY_PROPS)
    key = os.environ.get("ELEVENLABS_API_KEY") or p.get("ELEVENLABS_API_KEY")
    voice = os.environ.get("ELEVENLABS_VOICE_ID") or p.get("ELEVENLABS_VOICE_ID")
    if not key or not voice:
        sys.exit("ELEVENLABS_API_KEY / ELEVENLABS_VOICE_ID not found (env or Rozy_Assistant/local.properties)")
    ASSETS.mkdir(parents=True, exist_ok=True)
    for name, text in LINES.items():
        out = ASSETS / f"{name}.mp3"
        body = json.dumps({
            "text": text,
            "model_id": MODEL_ID,
            # Excited but premium: lower stability = livelier delivery, style adds
            # energy, high similarity + speaker boost keep it clean and on-brand.
            "voice_settings": {"stability": 0.32, "similarity_boost": 0.8, "style": 0.45, "use_speaker_boost": True},
        })
        r = subprocess.run([
            "curl", "-sS", "-f", "-o", str(out),
            f"https://api.elevenlabs.io/v1/text-to-speech/{voice}?output_format=mp3_44100_128",
            "-H", f"xi-api-key: {key}", "-H", "Content-Type: application/json",
            "--data", body,
        ])
        if r.returncode != 0:
            sys.exit(f"curl failed for {name}")
        print(f"{out.name}: {out.stat().st_size} bytes  ({text!r})")


if __name__ == "__main__":
    main()
