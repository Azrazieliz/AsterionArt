"""Exercise the isolated Studio APK on the GitHub Actions emulator."""

from pathlib import Path
import re
import shlex
import subprocess
import time
from xml.etree import ElementTree


SERIAL = "emulator-5554"
PACKAGE = "com.asterion.art.reconstruction"
ACTIVITY = f"{PACKAGE}/com.asterion.art.studio.StudioActivity"
EVIDENCE = Path("build/studio-qa")


def adb(*args: str, timeout: int = 15) -> bytes:
    return subprocess.check_output(["adb", "-s", SERIAL, *args], timeout=timeout)


def shell(*args: str) -> None:
    adb("shell", shlex.join(args))


def screen(label: str) -> list[ElementTree.Element]:
    response = adb("exec-out", "uiautomator", "dump", "/dev/tty", timeout=30)
    match = re.search(rb"<hierarchy\b.*?</hierarchy>", response, re.DOTALL)
    if match is None:
        raise RuntimeError(response.decode(errors="replace"))
    (EVIDENCE / f"{label}.xml").write_bytes(match[0])
    return list(ElementTree.fromstring(match[0]).iter("node"))


def shown(label: str, expected: str, *, contains: bool = False) -> ElementTree.Element:
    for _ in range(4):
        for node in screen(label):
            text = node.get("text", "") or node.get("content-desc", "")
            if text == expected or (contains and expected in text):
                return node
        time.sleep(.5)
    raise AssertionError(f"{expected!r} was missing at {label}.")


def tap(label: str, expected: str, *, contains: bool = False) -> None:
    node = shown(label, expected, contains=contains)
    left, top, right, bottom = map(int, re.findall(r"\d+", node.get("bounds", "")))
    shell("input", "tap", str((left + right) // 2), str((top + bottom) // 2))


def share_scene(text: str) -> None:
    shell("am", "start", "-W", "-n", ACTIVITY, "-a", "android.intent.action.SEND",
          "-t", "text/plain", "--es", "android.intent.extra.TEXT", text)


def screenshot(label: str) -> None:
    (EVIDENCE / f"{label}.png").write_bytes(adb("exec-out", "screencap", "-p"))


def main() -> None:
    shell("am", "start", "-W", "-n", ACTIVITY)
    tap("creation-modes", "Artwork", contains=True)
    shown("artwork-editor", "Start creation")
    tap("artwork-references", "Add reference")
    shown("reference-sheet", "Add Character Sheet")
    shown("reference-sources", "Browse files")
    screenshot("reference-picker")
    shell("input", "keyevent", "BACK")

    harbor = "Winter harbor at dawn. A signal lamp reflects across the breakwater."
    tower = "Inside a glass observatory, violet clouds race around the tower."
    share_scene(harbor)
    shown("writer-harbor-scene", harbor, contains=True)
    share_scene(tower)
    shown("writer-tower-scene", tower, contains=True)
    screenshot("animation-draft")
    tap("animation-draft", "Start creation")
    shown("queued-animation", "Blocked")
    shown("blocked-dependency", "production Anime runtime", contains=True)
    screenshot("queued-work")

    shell("am", "force-stop", PACKAGE)
    shell("am", "start", "-W", "-n", ACTIVITY)
    tap("restored-studio", "Projects")
    shown("restored-queue", "Blocked")
    shown("restored-first-writer-draft", "Winter harbor at dawn", contains=True)
    shown("restored-second-writer-draft", "Inside a glass observatory", contains=True)
    screenshot("restored-projects")


if __name__ == "__main__":
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    try:
        main()
    finally:
        (EVIDENCE / "logcat.txt").write_bytes(adb("logcat", "-d", "-s", "AndroidRuntime:E"))
