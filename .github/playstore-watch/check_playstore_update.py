import json
import os
from datetime import datetime, timezone
from pathlib import Path

import requests
from google_play_scraper import app as fetch_app
from google_play_scraper.exceptions import NotFoundError

PACKAGE_NAME = "com.dororong.rodi"
STORE_URL = f"https://play.google.com/store/apps/details?id={PACKAGE_NAME}"
STATE_FILE = Path(__file__).parent / "playstore-state.json"


def load_previous_state() -> dict:
    if STATE_FILE.exists():
        return json.loads(STATE_FILE.read_text())
    return {}


def save_state(state: dict) -> None:
    STATE_FILE.write_text(json.dumps(state, ensure_ascii=False, indent=2))


def fetch_current_state() -> dict:
    try:
        result = fetch_app(PACKAGE_NAME, lang="ko", country="kr")
    except NotFoundError:
        return {"live": False}
    return {"live": True, "version": result.get("version"), "updated": result.get("updated")}


def notify_discord(message: str) -> None:
    webhook_url = os.environ["DISCORD_WEBHOOK_URL"]
    resp = requests.post(webhook_url, json={"content": message}, timeout=10)
    resp.raise_for_status()


def build_notification(previous: dict, current: dict) -> str | None:
    was_live = previous.get("live", False)
    is_live = current["live"]

    if not was_live and is_live:
        return (
            "🎉 **Rodi 심사 승인! 플레이스토어에 게시되었습니다**\n"
            f"- 버전: {current.get('version')}\n"
            f"- 링크: {STORE_URL}"
        )

    if was_live and not is_live:
        return "⚠️ **Rodi가 플레이스토어에서 내려갔습니다** (정지/삭제 여부 확인 필요)"

    if was_live and is_live and previous.get("updated") != current.get("updated"):
        updated_at = datetime.fromtimestamp(current["updated"], tz=timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
        return (
            "📢 **Rodi 플레이스토어 업데이트 감지됨**\n"
            f"- 버전: {previous.get('version') or '알 수 없음'} → {current.get('version')}\n"
            f"- 반영 시각: {updated_at}\n"
            f"- 링크: {STORE_URL}"
        )

    return None


def main() -> None:
    current = fetch_current_state()
    previous = load_previous_state()

    if previous:
        message = build_notification(previous, current)
        if message:
            notify_discord(message)

    save_state(current)


if __name__ == "__main__":
    main()
