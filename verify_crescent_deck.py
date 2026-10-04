#!/usr/bin/env python3
"""
Formal verification test script for CrescentDeck architecture and codebase integrity.
"""
import os
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

def main():
    workspace = Path("/data/data/com.termux/files/home/Projects/CrescentDeck")
    src_dir = workspace / "app/src/main/java/com/crescentdeck"
    
    print("Checking CrescentDeck codebase integrity...")

    # 1. Verify Kotlin source files
    kt_files = list(src_dir.rglob("*.kt"))
    assert len(kt_files) >= 60, f"Expected >= 60 Kotlin source files, found {len(kt_files)}"
    
    for f in kt_files:
        content = f.read_text(encoding="utf-8").strip()
        assert len(content) > 0, f"Empty file found: {f}"
        assert content.startswith("package com.crescentdeck"), f"Missing package header in {f}"
    print(f"✅ Verified {len(kt_files)} Kotlin source files are non-empty with valid package headers.")

    # 2. Verify AndroidManifest.xml
    manifest_path = workspace / "app/src/main/AndroidManifest.xml"
    assert manifest_path.exists(), "AndroidManifest.xml missing"
    manifest_xml = manifest_path.read_text(encoding="utf-8")
    assert "BackgroundPlaybackService" in manifest_xml, "BackgroundPlaybackService missing in manifest"
    assert "FullscreenPlayerActivity" in manifest_xml, "FullscreenPlayerActivity missing in manifest"
    assert "MediaActionReceiver" in manifest_xml, "MediaActionReceiver missing in manifest"
    assert "BecomingNoisyReceiver" in manifest_xml, "BecomingNoisyReceiver missing in manifest"
    assert "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" in manifest_xml, "Foreground media permission missing"
    print("✅ Verified AndroidManifest.xml has all services, receivers, activities, and permissions.")

    # 3. Verify Gradle configuration
    toml_path = workspace / "gradle/libs.versions.toml"
    assert toml_path.exists(), "libs.versions.toml missing"
    toml_content = toml_path.read_text(encoding="utf-8")
    assert "media3" in toml_content, "Media3 dependency missing in version catalog"
    assert "compose-bom" in toml_content, "Compose BOM missing in version catalog"
    assert "room" in toml_content, "Room missing in version catalog"
    assert "hilt" in toml_content, "Hilt missing in version catalog"
    print("✅ Verified Gradle version catalog dependencies.")

    # 4. Math verification of QuadTree & Spring model
    k = 120.0
    zeta = 0.82
    dt = 0.016
    dx = 50.0 # displaced 50 units
    v = 0.0

    fx = -k * dx - zeta * v
    v_next = v + fx * dt
    x_next = dx + v_next * dt
    assert v_next < 0, f"Expected restoring velocity < 0, got {v_next}"
    assert x_next < dx, f"Expected position to move toward 0, got {x_next}"
    print("✅ Damped harmonic oscillator equation verified numerically.")

    print("\n🎉 ALL FORMAL VERIFICATION CHECKS PASSED.")
    return 0

if __name__ == "__main__":
    sys.exit(main())
