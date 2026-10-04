#!/usr/bin/env sh
set -eu

base="${APP_BASE_URL:-http://localhost:8080}"
curl --request POST "$base/legal-voice/collection"
curl --request POST "$base/legal-voice/index" \
  --header 'Content-Type: application/json' \
  --data '{"noteId":"note-104","matterId":"matter-27","kind":"SIGNED_DOCUMENT_DELIVERY","recordingRef":"call-2026-09-18","transcript":"Dana confirmed the settlement was signed. File the dismissal by September 22.","signed":true,"deadline":"2026-09-22"}'
curl --get "$base/legal-voice/search" --data-urlencode 'query=Which signed matter has a filing deadline?' --data-urlencode 'limit=3'
