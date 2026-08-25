# How to create a GitHub release for Desktop

1. Commit your changes
```bash
git add .
git commit -m "useful commit message"
git push
```

2. **Create and merge your pull request**
   Create a pull request on GitHub for your pushed commits. Once reviewed, merge the pull request into your primary branch (e.g., main).
3. **Update your local primary branch**
   Before tagging, you must pull the newly merged code back to your local machine so the tag points to the final, merged state.
```
git checkout main
git pull
```
4. **Create a tag with the release version**
Make sure the tag name starts with a lowercase "v" (e.g., v1.0.0). This tag determines the name of the release.

```
git tag -a v1.0.0 -m "New release"
```

5. **Push the tag to GitHub**
   Pushing the tag is what actually triggers the release workflow.

```
git push origin --tags
```

Pushing a `v*` tag triggers three workflows: `release_desktop_win.yml` (MSI), `release_desktop_linux.yml` (DEB/RPM/Arch), and `release_description.yml` (extracts the matching `### <version>` section from `README.md` as the release body).
