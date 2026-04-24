ARG PLAYWRIGHT_VERSION=1.44.1
FROM mcr.microsoft.com/playwright:v${PLAYWRIGHT_VERSION}-jammy

ARG TARGETARCH=amd64
ARG KUBECTL_VERSION=v1.30.1

RUN apt-get update && apt-get install -y --no-install-recommends \
    apache2-utils \
    ca-certificates \
    curl \
    git \
    jq \
  && rm -rf /var/lib/apt/lists/* \
  && curl -fsSL -o /usr/local/bin/kubectl "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/${TARGETARCH}/kubectl" \
  && chmod +x /usr/local/bin/kubectl

ENV CI=true \
    QA_PLAYWRIGHT_HOME=/opt/qa-playwright \
    PLAYWRIGHT_BROWSERS_PATH=/ms-playwright \
    npm_config_fund=false \
    npm_config_update_notifier=false

WORKDIR /opt/qa-playwright

COPY package.json package-lock.json playwright.config.js ./
COPY playwright-tests ./playwright-tests

RUN npm ci --no-fund --no-audit \
  && npm cache clean --force

WORKDIR /workspace

ENTRYPOINT ["/bin/bash", "-lc"]
CMD ["node --version && npm --version && npx playwright --version && kubectl version --client=true && jq --version && ab -V && test -d /opt/qa-playwright/playwright-tests"]
