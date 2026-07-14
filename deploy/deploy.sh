#!/usr/bin/env bash

set -Eeuo pipefail

readonly COMPOSE_FILE="docker-compose.yml"
readonly NEXT_ENV_FILE=".env.next"
readonly ACTIVE_ENV_FILE=".env"
readonly PREVIOUS_ENV_FILE=".env.previous"

if [[ ! -f "${COMPOSE_FILE}" ]]; then
  echo "Missing ${COMPOSE_FILE}."
  exit 1
fi

if [[ ! -f "${NEXT_ENV_FILE}" ]]; then
  echo "Missing ${NEXT_ENV_FILE}."
  exit 1
fi

chmod 600 "${NEXT_ENV_FILE}"

rollback() {
  local exit_code=$?
  trap - ERR

  echo "Deployment failed; restoring the previous Compose environment."
  if [[ -f "${PREVIOUS_ENV_FILE}" ]]; then
    mv -f "${PREVIOUS_ENV_FILE}" "${ACTIVE_ENV_FILE}"
    docker compose --env-file "${ACTIVE_ENV_FILE}" up -d --wait --wait-timeout 120 || true
  else
    echo "No previous deployment is available for rollback."
  fi

  exit "${exit_code}"
}

trap rollback ERR

docker compose --env-file "${NEXT_ENV_FILE}" pull

if [[ -f "${ACTIVE_ENV_FILE}" ]]; then
  cp -p "${ACTIVE_ENV_FILE}" "${PREVIOUS_ENV_FILE}"
fi

mv -f "${NEXT_ENV_FILE}" "${ACTIVE_ENV_FILE}"
docker compose --env-file "${ACTIVE_ENV_FILE}" up -d --wait --wait-timeout 120

rm -f "${PREVIOUS_ENV_FILE}"
trap - ERR

docker compose --env-file "${ACTIVE_ENV_FILE}" ps
