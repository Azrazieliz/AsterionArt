# Complete Git history archive

The dedicated migration branch contains source code and recovery docs; its workflow also
exports an entire branch-preserving Git bundle for independent, offline restoration.
Download the successful GitHub Actions artifact promptly (artifacts expire) and place it in
the larger private, user-facing recovery ZIP.

Use `git clone Asterion-Art-ALL-BRANCHES-HISTORY.bundle restore-art`, then
`git branch -a` and `git fsck --full` to validate. Current production source main
remains **88206baa9e2a33d41195834783023a9fb0c7e0dc**. Do not use backup
branch for feature work or force-update production refs. App-private user models,
proprietary binaries, user media and signing keys are not in the Git bundle.
