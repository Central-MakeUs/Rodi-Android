import importlib.util
import unittest
from pathlib import Path


SCRIPT_PATH = Path(__file__).with_name("check_playstore_update.py")
SPEC = importlib.util.spec_from_file_location("playstore_watch", SCRIPT_PATH)
playstore_watch = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(playstore_watch)


class PlayStoreWatchTest(unittest.TestCase):
    def test_finds_target_release_by_version_code(self) -> None:
        releases = [
            {
                "track": "production",
                "activeArtifacts": [{"versionCode": 5}],
                "releaseLifecycleState": "RELEASE_LIFECYCLE_STATE_PUBLISHED",
            },
            {
                "track": "production",
                "activeArtifacts": [{"versionCode": "6"}],
                "releaseLifecycleState": "RELEASE_LIFECYCLE_STATE_IN_REVIEW",
            },
        ]

        release = playstore_watch.find_target_release(releases, 6)

        self.assertEqual("RELEASE_LIFECYCLE_STATE_IN_REVIEW", release["releaseLifecycleState"])

    def test_returns_none_when_target_is_not_on_production_track(self) -> None:
        releases = [{"activeArtifacts": [{"versionCode": 5}]}]

        self.assertIsNone(playstore_watch.find_target_release(releases, 6))

    def test_notification_names_target_version_and_track(self) -> None:
        message = playstore_watch.build_notification(6, {"track": "production"})

        self.assertIn("versionCode: 6", message)
        self.assertIn("트랙: production", message)


if __name__ == "__main__":
    unittest.main()
