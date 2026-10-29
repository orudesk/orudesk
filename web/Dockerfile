# The web app's image: the built files, served by Caddy, which also passes /api to the API.
#   docker build -t orudesk-standard-web web/                  (from the repo root)
#   docker build --build-arg APP=standard -t ... web/          (APP picks the edition under apps/)

# ---- Stage 1: build the app with Node ----
FROM node:26-alpine AS build
WORKDIR /src
# The lockfile and package.json files first, so Docker reuses the install layer until dependencies change.
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
# Node 25 and later ship without corepack, so npm installs the pnpm version package.json names.
RUN npm install -g "$(node -p "require('./package.json').packageManager")"
COPY apps/standard/package.json apps/standard/
COPY packages/api-client/package.json packages/api-client/
COPY packages/web-core/package.json packages/web-core/
RUN --mount=type=cache,target=/root/.local/share/pnpm/store pnpm install --frozen-lockfile
COPY . .
ARG APP=standard
RUN pnpm --filter "./apps/${APP}" build && cp -r apps/${APP}/dist /dist

# ---- Stage 2: serve it with Caddy ----
FROM caddy:2-alpine
COPY Caddyfile /etc/caddy/Caddyfile
COPY --from=build /dist /srv
EXPOSE 80 443
# Asks Caddy's admin endpoint (local to the container): whatever ORUDESK_SITE is, it answers there.
# (Caddy runs as root in the official image to bind ports 80 and 443; it only reads the files it serves.)
HEALTHCHECK --interval=10s --timeout=3s --retries=3 CMD wget -q -O /dev/null http://127.0.0.1:2019/config/ || exit 1
