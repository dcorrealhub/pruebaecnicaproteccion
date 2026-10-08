#!/usr/bin/env bash
# Configuración compartida por los scripts de infraestructura.
set -euo pipefail

# Git Bash convierte argumentos que empiezan con "/" en rutas de Windows; la AWS CLI los necesita tal cual.
export MSYS_NO_PATHCONV=1

export REGION="${REGION:-us-east-2}"
export AWS_REGION="$REGION" AWS_DEFAULT_REGION="$REGION"
export AWS_PAGER=""

INFRA_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RETO_B_DIR="$(dirname "$INFRA_DIR")"
ESTADO="$INFRA_DIR/estado.env"

PROYECTO="reto-b"
CUENTA="$(aws sts get-caller-identity --query Account --output text)"
BUCKET_FRONT="${PROYECTO}-front-${CUENTA}-${REGION}"
BUCKET_ARTEFACTOS="${PROYECTO}-artefactos-${CUENTA}-${REGION}"
ROL_EC2="${PROYECTO}-ec2-rol"
TAGS_EC2="{Key=Project,Value=${PROYECTO}}"

[ -f "$ESTADO" ] && source "$ESTADO"

# Registra el id de un recurso creado para poder reanudar el despliegue o destruirlo
guardar() {
  echo "$1=$2" >> "$ESTADO"
  printf -v "$1" '%s' "$2"
}

# Ruta que entiende la AWS CLI de Windows en parámetros file://
ruta_cli() {
  if command -v cygpath >/dev/null; then cygpath -m "$1"; else echo "$1"; fi
}

paso() { printf '\n==> %s\n' "$*"; }
