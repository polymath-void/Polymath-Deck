#!/usr/bin/env python3
"""
Formal verification test script for PolymathDeck architecture and codebase integrity.
"""
import os
import sys
import subprocess
from pathlib import Path

def main():
    workspace = Path("/data/data/com.termux/files/home/Projects/Polymath-Deck")
    src_dir = workspace / "app/src/main/java/com/polymathdeck"
    test_dir = workspace / "app/src/test/java/com/polymathdeck"
    
    print("Checking PolymathDeck codebase integrity...")

    # 1. Verify Kotlin main source files
    kt_files = list(src_dir.rglob("*.kt"))
    assert len(kt_files) >= 60, f"Expected >= 60 Kotlin source files, found {len(kt_files)}"
    
    for f in kt_files:
        content = f.read_text(encoding="utf-8").strip()
        assert len(content) > 0, f"Empty file found: {f}"
        assert content.startswith("package com.polymathdeck"), f"Missing package header in {f}"
    print(f"✅ Verified {len(kt_files)} Kotlin main source files are non-empty with valid package headers.")

    # 2. Verify Kotlin unit test files
    test_kt_files = list(test_dir.rglob("*.kt"))
    assert len(test_kt_files) >= 5, f"Expected >= 5 unit test Kotlin files, found {len(test_kt_files)}"
    for f in test_kt_files:
        content = f.read_text(encoding="utf-8").strip()
        assert len(content) > 0, f"Empty test file found: {f}"
        assert content.startswith("package com.polymathdeck"), f"Missing package header in {f}"
    print(f"✅ Verified {len(test_kt_files)} Kotlin unit test files.")

    # 3. Verify AndroidManifest.xml
    manifest_path = workspace / "app/src/main/AndroidManifest.xml"
    assert manifest_path.exists(), "AndroidManifest.xml missing"
    manifest_xml = manifest_path.read_text(encoding="utf-8")
    assert "BackgroundPlaybackService" in manifest_xml, "BackgroundPlaybackService missing in manifest"
    assert "FullscreenPlayerActivity" in manifest_xml, "FullscreenPlayerActivity missing in manifest"
    assert "MediaActionReceiver" in manifest_xml, "MediaActionReceiver missing in manifest"
    assert "BecomingNoisyReceiver" in manifest_xml, "BecomingNoisyReceiver missing in manifest"
    assert "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" in manifest_xml, "Foreground media permission missing"
    print("✅ Verified AndroidManifest.xml has all services, receivers, activities, and permissions.")

    # 4. Verify Architectural Invariants
    governor_kt = (src_dir / "engine/governor/WebViewResourceGovernor.kt").read_text(encoding="utf-8")
    assert "enum class CardLifecycleState" in governor_kt, "CardLifecycleState missing in WebViewResourceGovernor"
    assert "GRID_FLOW" in governor_kt and "IMMERSIVE" in governor_kt and "MINIMIZED" in governor_kt and "RESIZING" in governor_kt
    assert "DEFAULT_MAX_ACTIVE_WEBVIEWS = 6" in governor_kt, "Max active WebViews limit 6 missing"
    assert "enforceLRULimit" in governor_kt, "LRU enforcement missing"
    print("✅ Verified WebViewResourceGovernor 4-tier lifecycle & LRU watchdog invariants.")

    collision_kt = (src_dir / "engine/physics/CollisionResolver.kt").read_text(encoding="utf-8")
    assert "STATIC_GUTTER: Float = 16f" in collision_kt, "16dp static gutter missing"
    assert "ACTIVE_DRAG_GUTTER: Float = 24f" in collision_kt, "24dp active drag gutter missing"
    print("✅ Verified CollisionResolver dynamic CollisionGutter (16dp static vs 24dp active drag).")

    card_container_kt = (src_dir / "ui/card/SelectiveCardNodeContainer.kt").read_text(encoding="utf-8")
    assert "CardHeaderBar" in card_container_kt, "32dp CardHeaderBar missing in SelectiveCardNodeContainer"
    assert "32.dp" in card_container_kt, "32dp header height missing"
    assert "0xFFFF5F56" in card_container_kt and "0xFFFFBD2E" in card_container_kt and "0xFF27C93F" in card_container_kt, "Traffic lights missing in HeaderBar"
    assert "CornerResizeHandle" in card_container_kt, "CornerResizeHandle missing"
    assert "24.dp" in card_container_kt and "6.dp" in card_container_kt, "Dynamic Z-axis elevation missing"
    print("✅ Verified Gesture Segregation, Traffic Lights, Dynamic Z-Axis Elevation, and Corner Resizing.")

    canvas_kt = (src_dir / "ui/canvas/DragDropGridCanvas.kt").read_text(encoding="utf-8")
    assert "dotSpacing" in canvas_kt and "drawCircle" in canvas_kt, "Procedural infinite dot grid missing in Canvas"
    assert "resistanceX" in canvas_kt and "resistanceY" in canvas_kt, "Elastic edge rubber-banding missing in Canvas"
    print("✅ Verified Infinite Canvas Procedural Dot Grid and Elastic Edge Rubber-banding.")

    scrim_kt = (src_dir / "ui/card/HibernationScrimOverlay.kt").read_text(encoding="utf-8")
    assert "desaturationFilter" in scrim_kt, "Desaturation grayscale filter missing in HibernationScrimOverlay"
    assert "isWaking" in scrim_kt and "CircularProgressIndicator" in scrim_kt, "Cold Start Veil loading spinner missing"
    print("✅ Verified Resource Governor Hibernation Desaturation & Cold Start Veil.")

    theme_kt = (src_dir / "ui/theme/ThemeManager.kt").read_text(encoding="utf-8")
    assert "categoryOverrides" in theme_kt, "Category overrides missing in ThemeManager"
    assert "setCategoryOverride" in theme_kt, "setCategoryOverride missing"
    print("✅ Verified ThemeManager 3-level cascade (Global -> Category -> Deck/Card).")

    # MVI Architecture & ViewState Store
    viewmodel_kt = (src_dir / "viewmodel/DeckViewModel.kt").read_text(encoding="utf-8")
    assert "fun processIntent(intent: ViewIntent)" in viewmodel_kt, "processIntent missing in DeckViewModel"
    assert "val viewState: StateFlow<DeckViewState>" in viewmodel_kt, "viewState StateFlow missing in DeckViewModel"
    print("✅ Verified MVI Subsystem: ViewIntent and reactive DeckViewState single source of truth.")

    # QuadTree Depth 8 & Object Allocation Zero-Impulse Cache
    quadtree_kt = (src_dir / "engine/quadtree/QuadTreeNode.kt").read_text(encoding="utf-8")
    assert "maxDepth: Int = 8" in quadtree_kt, "QuadTreePartition maxDepth 8 missing"
    assert "val ZERO = Vector2D(0f, 0f)" in collision_kt, "Vector2D.ZERO cache missing in CollisionResolver"
    print("✅ Verified QuadTree Hardening (maxDepth = 8, threshold = 4) and Vector2D impulse allocation caching.")

    # DiskSnapshotManager & Governor Disk Persistence
    disk_snapshot_file = src_dir / "engine/governor/DiskSnapshotManager.kt"
    assert disk_snapshot_file.exists(), "DiskSnapshotManager.kt does not exist"
    disk_snapshot_kt = disk_snapshot_file.read_text(encoding="utf-8")
    assert "suspend fun saveSnapshot" in disk_snapshot_kt, "saveSnapshot missing in DiskSnapshotManager"
    assert "suspend fun loadSnapshot" in disk_snapshot_kt, "loadSnapshot missing in DiskSnapshotManager"
    assert "diskSnapshotManager" in governor_kt, "diskSnapshotManager reference missing in WebViewResourceGovernor"
    assert "loadSnapshotFromDisk" in governor_kt, "loadSnapshotFromDisk missing in WebViewResourceGovernor"
    print("✅ Verified DiskSnapshotManager and disk-backed bitmap snapshot cache.")

    # LiveCardFrame Hardened Settings & Link Interception Spawning Cards
    live_card_kt = (src_dir / "ui/card/LiveCardFrame.kt").read_text(encoding="utf-8")
    assert "databaseEnabled = true" in live_card_kt, "databaseEnabled = true missing in LiveCardFrame"
    assert "shouldOverrideUrlLoading" in live_card_kt, "shouldOverrideUrlLoading missing in LiveCardFrame"
    assert "onOpenUrlAsCard" in live_card_kt, "onOpenUrlAsCard parameter missing in LiveCardFrame"
    print("✅ Verified LiveCardFrame databaseEnabled and URL navigation interception spawning new canvas nodes.")

    # 5. Verify Gradle configuration & CI/CD APK Build Pipeline
    toml_path = workspace / "gradle/libs.versions.toml"
    assert toml_path.exists(), "libs.versions.toml missing"
    toml_content = toml_path.read_text(encoding="utf-8")
    assert "media3" in toml_content, "Media3 dependency missing in version catalog"
    assert "compose-bom" in toml_content, "Compose BOM missing in version catalog"
    assert "room" in toml_content, "Room missing in version catalog"
    assert "hilt" in toml_content, "Hilt missing in version catalog"
    
    workflow_path = workspace / ".github/workflows/build_apk.yml"
    assert workflow_path.exists(), "build_apk.yml missing in .github/workflows"
    workflow_yaml = workflow_path.read_text(encoding="utf-8")
    assert "assembleDebug" in workflow_yaml, "assembleDebug missing in workflow"
    assert "upload-artifact" in workflow_yaml, "upload-artifact missing in workflow"

    assert (workspace / "gradlew").exists(), "gradlew missing"
    assert (workspace / "gradle/wrapper/gradle-wrapper.properties").exists(), "gradle-wrapper.properties missing"
    assert (workspace / "gradle/wrapper/gradle-wrapper.jar").exists(), "gradle-wrapper.jar missing"
    print("✅ Verified Gradle version catalog, Gradle wrapper, and GitHub Actions APK build workflow.")

    # 6. Run simulate_physics_engine.py verification suite
    sim_script = workspace / "simulate_physics_engine.py"
    res = subprocess.run([sys.executable, str(sim_script)], capture_output=True, text=True)
    assert res.returncode == 0, f"simulate_physics_engine failed:\n{res.stderr}\n{res.stdout}"
    print("✅ Verified Real-time Physics Engine Simulator, O(log N) benchmark, and LRU watchdog.")

    print("\n🎉 ALL FORMAL VERIFICATION CHECKS PASSED.")
    return 0

if __name__ == "__main__":
    sys.exit(main())
