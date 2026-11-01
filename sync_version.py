#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os
import re
import sys

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

PROJECT_DIR = os.path.dirname(os.path.abspath(__file__))
CHANGELOG_PATH = os.path.join(PROJECT_DIR, "CHANGELOG.md")

def get_latest_version():
    if not os.path.exists(CHANGELOG_PATH):
        return None, None
    with open(CHANGELOG_PATH, "r", encoding="utf-8") as f:
        content = f.read()
    # 匹配 ## [1.0.0] 或 ## v1.0.0
    match = re.search(r'##\s*\[?v?(\d+\.\d+\.\d+)\]?', content)
    if not match: 
        return None, None
    version_name = match.group(1)
    parts = version_name.split('.')
    # 生成 versionCode，例如 1.0.0 变 10000
    version_code = str(int(parts[0]) * 10000 + int(parts[1]) * 100 + int(parts[2]))
    return version_name, version_code

def update_file(rel_path, pattern, replacement):
    full = os.path.join(PROJECT_DIR, rel_path)
    if not os.path.exists(full): 
        print(f"[SKIP] File not found: {rel_path}")
        return
    with open(full, "r", encoding="utf-8") as f: 
        content = f.read()
    
    new_content = re.sub(pattern, replacement, content)
    if new_content != content:
        with open(full, "w", encoding="utf-8", newline="\n") as f: 
            f.write(new_content)
        print(f"[OK] Synced: {rel_path}")
    else:
        print(f"[WARN] No changes needed: {rel_path}")

if __name__ == "__main__":
    print("Checking CHANGELOG.md...")
    version_name, version_code = get_latest_version()
    if not version_name:
        print("❌ No valid version found in CHANGELOG.md")
        sys.exit(1)
        
    print(f"✅ Found version: v{version_name} (Code: {version_code})")

    # 同步 Gradle 版本号
    update_file("app/build.gradle", r'versionCode\s+\d+', f'versionCode {version_code}')
    update_file("app/build.gradle", r'versionName\s+["\'][^"\']+["\']', f'versionName "{version_name}"')

    print("🎉 Version sync completed successfully.")
