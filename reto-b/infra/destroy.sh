#!/usr/bin/env bash
# Elimina TODOS los recursos creados por deploy.sh (incluidos los datos de PostgreSQL de la EC2).
source "$(dirname "$0")/comun.sh"

read -r -p "Esto borra la distribución, la EC2 (y su base de datos) y los buckets de reto-b. ¿Continuar? (si/no) " RESP
[ "$RESP" = "si" ] || { echo "Cancelado"; exit 1; }

TMP="$INFRA_DIR/.tmp"
mkdir -p "$TMP"

if [ -n "${DISTRIBUTION_ID:-}" ]; then
  paso "Deshabilitando distribución $DISTRIBUTION_ID (requisito para borrarla)"
  ETAG="$(aws cloudfront get-distribution-config --id "$DISTRIBUTION_ID" --query ETag --output text)"
  aws cloudfront get-distribution-config --id "$DISTRIBUTION_ID" --query DistributionConfig --output json \
    | sed 's/"Enabled": true/"Enabled": false/' > "$TMP/config.json"
  if grep -q '"Enabled": false' "$TMP/config.json"; then
    ETAG="$(aws cloudfront update-distribution --id "$DISTRIBUTION_ID" --if-match "$ETAG" \
      --distribution-config "file://$(ruta_cli "$TMP/config.json")" --query ETag --output text)"
  fi
  echo "Esperando que se propague (5-15 min)..."
  aws cloudfront wait distribution-deployed --id "$DISTRIBUTION_ID"
  ETAG="$(aws cloudfront get-distribution --id "$DISTRIBUTION_ID" --query ETag --output text)"
  aws cloudfront delete-distribution --id "$DISTRIBUTION_ID" --if-match "$ETAG"
fi

if [ -n "${OAC_ID:-}" ]; then
  paso "Borrando Origin Access Control"
  ETAG="$(aws cloudfront get-origin-access-control --id "$OAC_ID" --query ETag --output text)"
  aws cloudfront delete-origin-access-control --id "$OAC_ID" --if-match "$ETAG"
fi

for BUCKET in "$BUCKET_FRONT" "$BUCKET_ARTEFACTOS"; do
  if aws s3api head-bucket --bucket "$BUCKET" 2>/dev/null; then
    paso "Borrando bucket $BUCKET"
    aws s3 rb "s3://$BUCKET" --force
  fi
done

if [ -n "${INSTANCE_ID:-}" ]; then
  paso "Terminando instancia $INSTANCE_ID"
  aws ec2 terminate-instances --instance-ids "$INSTANCE_ID" >/dev/null
  aws ec2 wait instance-terminated --instance-ids "$INSTANCE_ID"
fi

if [ -n "${SG_ID:-}" ]; then
  paso "Borrando security group $SG_ID"
  aws ec2 delete-security-group --group-id "$SG_ID"
fi

if aws iam get-role --role-name "$ROL_EC2" >/dev/null 2>&1; then
  paso "Borrando rol IAM $ROL_EC2"
  aws iam remove-role-from-instance-profile --instance-profile-name "$ROL_EC2" --role-name "$ROL_EC2" || true
  aws iam delete-instance-profile --instance-profile-name "$ROL_EC2" || true
  aws iam detach-role-policy --role-name "$ROL_EC2" --policy-arn arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore || true
  aws iam delete-role-policy --role-name "$ROL_EC2" --policy-name leer-artefactos || true
  aws iam delete-role --role-name "$ROL_EC2"
fi

rm -rf "$TMP" "$ESTADO"
paso "Todos los recursos de reto-b fueron eliminados"
