# XmlLanguageTranslator

Desktop tool (Kotlin Multiplatform / Compose Multiplatform, JVM target) for editing Android `strings.xml` translations side by side. Load one language file per column, edit values in a grid, and keep every file's structure (keys, comments, ordering) in sync.

## Features

- **Multi-file grid** — open several `strings.xml` files (one per language) and edit their values side by side in one table.
- **Master structure** — the first loaded file defines the key order, comments, and structure. Missing keys from other files are merged in automatically.
- **Sync & Save All** — propagates the master structure to every open file (adding missing keys as empty values) and writes all files to disk in one click.
- **Untranslated filter** — toggle a filter to show only keys that are empty in at least one file.
- **Search** — filter rows by key name or value across all open files.
- **Row editing** — reorder entries (up/down), insert comments above a row, delete a key from all files.
- **Reload from disk** — discard in-memory edits and re-parse all open files from disk.
- **Reopens last session** — files you had open are remembered and reloaded automatically the next time you start the app.

## Requirements

- JDK 17+
- Android Studio or IntelliJ IDEA with the Kotlin Multiplatform plugin (recommended), or the Gradle wrapper from the command line.

## Running

### From Android Studio / IntelliJ

1. Open the project.
2. Add/select a Gradle run configuration.
3. Run input field: `runDesktop`.
4. Run it.

### From the command line

```bash
./gradlew runDesktop
```

## Usage

1. **Load the default file first.** Its key order and structure become the master structure and are preserved when saving.
2. **Add the other language files** with the same "Add File" action.
3. Edit values directly in the grid. Empty values are highlighted.
4. Use the **untranslated filter** (top right) to focus on missing translations.
5. Click **Save All** to sync structure across files and write everything to disk.

The app remembers which files were open and reopens them automatically on next launch.

## Project structure

```
composeApp/src/jvmMain/kotlin/org/lerchenflo/xmllanguagetranslator/
├── main.kt        # Application entry point / window setup
├── App.kt         # Main UI: grid, toolbar, filtering, search, row actions
├── FilePicker.kt  # Native file chooser + persisted "last directory" / "open files"
├── ProjectFile.kt # In-memory model of an opened strings.xml file
├── XmlNode.kt     # Parsed XML node types (string entry, comment, whitespace, other)
└── XmlUtils.kt    # XML parsing and serialization
```

## Build distributable

```bash
./gradlew packageDistributionForCurrentOS
```

Produces a native installer (`.dmg` on macOS, `.msi` on Windows, `.deb`/`.rpm` on Linux) under `composeApp/build/compose/binaries`.

## Releasing

Pushing a `v*` tag (e.g. `v1.0.0`) triggers the release workflows under `.github/workflows/`, which build the Windows MSI, Linux DEB/RPM/Arch packages, and publish a GitHub Release. The release body is pulled from the matching `### <version>` section below. See `.github/workflows/workflow_release.md` for the full tagging steps.

## Changelog

### 1.0.0
- Initial release.
