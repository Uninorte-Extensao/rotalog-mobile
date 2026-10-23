"""Contratos da distribuição do APK pelo GitHub Actions."""

from pathlib import Path
import unittest

import yaml


ROOT = Path(__file__).resolve().parents[2]


class AndroidWorkflowTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        # BaseLoader preserva a chave `on` (YAML 1.1 a interpreta como booleano).
        cls.workflow = yaml.load(
            (ROOT / ".github/workflows/ci.yml").read_text(), Loader=yaml.BaseLoader
        )
        cls.steps = cls.workflow["jobs"]["android"]["steps"]

    def uploads_for(self, path):
        return [
            step for step in self.steps
            if step.get("uses", "").startswith("actions/upload-artifact@")
            and step["with"]["path"].strip() == path
        ]

    def test_main_push_and_manual_build_are_available(self):
        events = self.workflow["on"]
        self.assertIn("main", events["push"]["branches"])
        self.assertIn("workflow_dispatch", events)
        self.assertIn("pull_request", events)

    def test_android_sdk_setup_does_not_request_removed_tools_package(self):
        setup = next(s for s in self.steps
                     if s.get("uses", "").startswith("android-actions/setup-android@"))
        # setup-android v3 usa `tools platform-tools` quando não há configuração.
        packages = setup.get("with", {}).get("packages", "tools platform-tools").split()
        self.assertNotIn("tools", packages, "O pacote legado tools foi removido do SDK")
        self.assertIn("platform-tools", packages)

    def test_apk_download_is_separate_and_requires_successful_checks(self):
        apks = self.uploads_for("app/build/outputs/apk/debug/app-debug.apk")
        self.assertEqual(len(apks), 1, "Disponibilizar o APK em um artifact próprio")
        apk = apks[0]
        self.assertIn(apk.get("if", "success()"), ("success()", "${{ success() }}"))
        self.assertEqual(apk["with"]["if-no-files-found"], "error")
        self.assertEqual(apk["with"]["retention-days"], "30")
        self.assertIn("github.sha", apk["with"]["name"])
        preceding = self.steps[:self.steps.index(apk)]
        commands = "\n".join(s.get("run", "") for s in preceding)
        for task in (":app:assembleDebug", ":app:testDebugUnitTest", ":app:lintDebug"):
            self.assertIn(task, commands)
        self.assertFalse(any(s.get("continue-on-error") == "true" for s in preceding))

    def test_reports_remain_available_when_checks_fail(self):
        reports = self.uploads_for("app/build/reports/")
        self.assertEqual(len(reports), 1, "Preservar relatórios separados para diagnóstico")
        self.assertIn(reports[0].get("if"), ("always()", "${{ always() }}"))


if __name__ == "__main__":
    unittest.main()
