# The packages, as an app outside this workspace gets them

`pnpm test:packages` (from `web/`) packs `@orudesk/web-core` and `@orudesk/api-client` exactly as
`npm publish` would, installs the tarballs into a copy of this folder with **npm** (not pnpm, not the
workspace), and then type-checks, tests and builds this small app against them.

It catches what the workspace hides: a file missing from `files`, an export that points at `src/`, a
dependency that only resolved because the workspace had it, styles that didn't make it into the package.

This folder is deliberately **not** a workspace package. Its `package.json` is written by the script.
