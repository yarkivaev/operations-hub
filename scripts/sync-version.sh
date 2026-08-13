#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
version_file="$root/VERSION"
client_dir="$root/plan-http-client"
package_json="$client_dir/package.json"
package_lock="$client_dir/package-lock.json"
compat_json="$client_dir/compat.json"
mode="${1:-sync}"

if [ ! -f "$version_file" ]; then
  echo "VERSION file missing at $version_file" >&2
  exit 1
fi

version="$(tr -d '[:space:]' < "$version_file")"
if [ -z "$version" ]; then
  echo "VERSION file is empty" >&2
  exit 1
fi

read_json_version() {
  local file="$1"
  grep -m1 '"version"' "$file" | sed -E 's/^[^"]*"version"[[:space:]]*:[[:space:]]*"([^"]+)".*/\1/'
}

check_sync() {
  local issues=0
  local pkg_ver lock_ver

  pkg_ver="$(read_json_version "$package_json")"
  lock_ver="$(read_json_version "$package_lock")"

  if [ "$pkg_ver" != "$version" ]; then
    echo "plan-http-client/package.json version ${pkg_ver} != VERSION ${version}" >&2
    issues=1
  fi
  if [ "$lock_ver" != "$version" ]; then
    echo "plan-http-client/package-lock.json version ${lock_ver} != VERSION ${version}" >&2
    issues=1
  fi
  if ! grep -Eq "\"${version}\"" "$compat_json"; then
    echo "plan-http-client/compat.json missing serverTags entry ${version}" >&2
    issues=1
  fi
  if [ "$issues" -ne 0 ]; then
    echo "Run ./scripts/sync-version.sh to align derived files with VERSION" >&2
    exit 1
  fi
  echo "version ${version} is in sync"
}

run_sync() {
  if command -v python3 >/dev/null 2>&1; then
    python3 - "$version" "$package_json" "$package_lock" "$compat_json" <<'PY'
import json
import sys

version, package_json, package_lock, compat_json = sys.argv[1:5]

def load(path):
    with open(path, encoding="utf-8") as handle:
        return json.load(handle)

def dump(path, value):
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(value, handle, indent=2)
        handle.write("\n")

package_doc = load(package_json)
package_doc["version"] = version
dump(package_json, package_doc)

lock_doc = load(package_lock)
lock_doc["version"] = version
if isinstance(lock_doc.get("packages"), dict) and "" in lock_doc["packages"]:
    lock_doc["packages"][""]["version"] = version
dump(package_lock, lock_doc)

compat_doc = load(compat_json)
tags = set(compat_doc.get("serverTags", []))
tags.add(version)
compat_doc["serverTags"] = sorted(tags)
dump(compat_json, compat_doc)
PY
  elif command -v node >/dev/null 2>&1; then
    node <<EOF
const fs = require('node:fs');
const version = '${version}';
const packageJsonPath = '${package_json}';
const packageLockPath = '${package_lock}';
const compatJsonPath = '${compat_json}';
function readJson(path) { return JSON.parse(fs.readFileSync(path, 'utf8')); }
function writeJson(path, value) { fs.writeFileSync(path, \`\${JSON.stringify(value, null, 2)}\n\`); }
const packageDoc = readJson(packageJsonPath);
packageDoc.version = version;
writeJson(packageJsonPath, packageDoc);
const lockDoc = readJson(packageLockPath);
lockDoc.version = version;
if (lockDoc.packages && lockDoc.packages['']) lockDoc.packages[''].version = version;
writeJson(packageLockPath, lockDoc);
const compatDoc = readJson(compatJsonPath);
const tags = new Set(Array.isArray(compatDoc.serverTags) ? compatDoc.serverTags : []);
tags.add(version);
compatDoc.serverTags = [...tags].sort();
writeJson(compatJsonPath, compatDoc);
EOF
  else
    echo "sync requires python3 or node on PATH" >&2
    exit 1
  fi
  echo "synced derived files to version ${version}"
}

case "$mode" in
  --check) check_sync ;;
  sync) run_sync ;;
  *) echo "usage: $0 [--check|sync]" >&2; exit 1 ;;
esac
