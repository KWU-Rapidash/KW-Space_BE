#!/usr/bin/env bash

set -euo pipefail

cleanup() {
	unset KLAS_E2E_ID KLAS_E2E_PASSWORD
}
trap cleanup EXIT

read -r -p "KLAS ID: " KLAS_E2E_ID
read -r -s -p "KLAS password: " KLAS_E2E_PASSWORD
printf '\n'

if [[ -z "${KLAS_E2E_ID}" || -z "${KLAS_E2E_PASSWORD}" ]]; then
	printf 'KLAS credentials must not be blank.\n' >&2
	exit 1
fi

export KLAS_E2E_ID KLAS_E2E_PASSWORD
exec ./gradlew --no-daemon klasE2e
