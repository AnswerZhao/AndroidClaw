#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
UI_DIR="$ROOT_DIR/app/src/main/java/com/zeroclaw/android/ui"

if [[ ! -d "$UI_DIR" ]]; then
  echo "UI source directory not found: $UI_DIR"
  exit 1
fi

declare -a PATTERNS=(
  'Text\(\s*"'
  '\btext\s*=\s*"'
  '\btitle\s*=\s*"'
  '\bsubtitle\s*=\s*"'
  '\bcontentDescription\s*=\s*"'
)

tmp_matches="$(mktemp)"
trap 'rm -f "$tmp_matches"' EXIT

for pattern in "${PATTERNS[@]}"; do
  rg -n --pcre2 "$pattern" "$UI_DIR" --glob '*.kt' >> "$tmp_matches" || true
done

# Allow explicit one-line opt-out for exceptional cases.
if [[ -s "$tmp_matches" ]]; then
  grep -v 'i18n-ignore' "$tmp_matches" > "${tmp_matches}.filtered" || true
  mv "${tmp_matches}.filtered" "$tmp_matches"
fi

if [[ -s "$tmp_matches" ]]; then
  echo "Found hardcoded UI literals that should use string resources:"
  sort -u "$tmp_matches"
  exit 1
fi

echo "i18n UI hardcoded literal check passed."
