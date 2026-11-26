"""Copies the widget code into the generated Android project and registers the widgets.
Run by the GitHub Actions build after `npx cap add android`."""
import pathlib, shutil, re

root = pathlib.Path(__file__).resolve().parent
app = root.parent / "android" / "app" / "src" / "main"
java = app / "java" / "com" / "mocacad" / "dailydally"

for f in (root / "java").glob("*.java"):
    shutil.copy(f, java / f.name)
for f in (root / "res").rglob("*.xml"):
    dest = app / "res" / f.relative_to(root / "res")
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy(f, dest)

manifest = app / "AndroidManifest.xml"
text = manifest.read_text()
receivers = ""
for name, label, res in [("DDWidgetLarge", "DailyDally (large)", "dd_widget_large"),
                         ("DDWidgetSmall", "DailyDally streak", "dd_widget_small")]:
    receivers += f'''
        <receiver android:name=".{name}" android:exported="false" android:label="{label}">
            <intent-filter><action android:name="android.appwidget.action.APPWIDGET_UPDATE" /></intent-filter>
            <meta-data android:name="android.appwidget.provider" android:resource="@xml/{res}" />
        </receiver>'''
if "DDWidgetLarge" not in text:
    text = re.sub(r"(\s*)</application>", receivers + r"\1</application>", text, count=1)
    manifest.write_text(text)
print("Widgets installed")
