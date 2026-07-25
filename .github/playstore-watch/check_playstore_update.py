import json
import os
import re
from pathlib import Path
from typing import Optional

import requests

PACKAGE_NAME = "com.dororong.rodi"
TRACK_NAME = "production"
STORE_URL = f"https://play.google.com/store/apps/details?id={PACKAGE_NAME}"
STATE_FILE = Path(__file__).parent / "playstore-state.json"
BUILD_FILE = Path("app/build.gradle.kts")
PUBLISHED_STATE = "RELEASE_LIFECYCLE_STATE_PUBLISHED"
PLAY_SCOPE = "https://www.googleapis.com/auth/androidpublisher"


def load_state() -> dict:
    return json.loads(STATE_FILE.read_text())


def save_state(state: dict) -> None:
    STATE_FILE.write_text(json.dumps(state, ensure_ascii=False, indent=2) + "\n")


def read_target_version_code() -> int:
    match = re.search(r"versionCode\s*=\s*(\d+)", BUILD_FILE.read_text())
    if not match:
        raise RuntimeError(f"versionCode not found in {BUILD_FILE}")
    return int(match.group(1))


def fetch_production_releases(service_account_json: str) -> list[dict]:
    from google.auth.transport.requests import AuthorizedSession
    from google.oauth2 import service_account

    credentials = service_account.Credentials.from_service_account_info(
        json.loads(service_account_json),
        scopes=[PLAY_SCOPE],
    )
    url = (
        "https://androidpublisher.googleapis.com/androidpublisher/v3/"
        f"applications/{PACKAGE_NAME}/tracks/{TRACK_NAME}/releases"
    )
    response = AuthorizedSession(credentials).get(url, timeout=20)
    response.raise_for_status()
    return response.json().get("releases", [])


def find_target_release(releases: list[dict], target_version_code: int) -> Optional[dict]:
    for release in releases:
        version_codes = {
            int(artifact["versionCode"])
            for artifact in release.get("activeArtifacts", [])
            if "versionCode" in artifact
        }
        if target_version_code in version_codes:
            return release
    return None


def build_notification(target_version_code: int, release: dict) -> str:
    return (
        "📢 **Rodi 플레이스토어 출시 감지됨**\n"
        f"- versionCode: {target_version_code}\n"
        f"- 트랙: {release.get('track', TRACK_NAME)}\n"
        f"- 링크: {STORE_URL}"
    )


def notify_discord(message: str) -> None:
    webhook_url = os.environ["DISCORD_WEBHOOK_URL"]
    response = requests.post(webhook_url, json={"content": message}, timeout=10)
    response.raise_for_status()


def main() -> None:
    service_account_json = os.environ.get("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON")
    if not service_account_json:
        raise RuntimeError("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON secret is required")

    state = load_state()
    target_version_code = read_target_version_code()
    last_notified_version_code = state["lastNotifiedVersionCode"]

    print(f"Target versionCode: {target_version_code}")
    print(f"Last notified versionCode: {last_notified_version_code}")

    if target_version_code <= last_notified_version_code:
        print("No newer release version to monitor")
        return

    releases = fetch_production_releases(service_account_json)
    target_release = find_target_release(releases, target_version_code)
    if not target_release:
        print("Target version is not available on the production track yet")
        return

    lifecycle_state = target_release.get("releaseLifecycleState")
    print(f"Target lifecycle state: {lifecycle_state}")
    if lifecycle_state != PUBLISHED_STATE:
        print("Target version is not published yet")
        return

    notify_discord(build_notification(target_version_code, target_release))
    state["lastNotifiedVersionCode"] = target_version_code
    save_state(state)
    print("Discord notification sent")


if __name__ == "__main__":
    main()
