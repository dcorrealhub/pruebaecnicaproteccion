#!/usr/bin/env bash
# Compila el frontend, lo sube a S3 e invalida index.html en CloudFront.
source "$(dirname "$0")/comun.sh"
: "${DISTRIBUTION_ID:?Primero ejecuta deploy.sh}"

paso "Compilando frontend"
(cd "$RETO_B_DIR/frontend" && env -u MSYS_NO_PATHCONV npm run build --silent)
DIST_DIR="$RETO_B_DIR/frontend/dist"

paso "Subiendo a s3://$BUCKET_FRONT"
aws s3 sync "$(ruta_cli "$DIST_DIR/assets")" "s3://$BUCKET_FRONT/assets" --delete --only-show-errors \
  --cache-control "public,max-age=31536000,immutable"
aws s3 cp "$(ruta_cli "$DIST_DIR/index.html")" "s3://$BUCKET_FRONT/index.html" --only-show-errors \
  --cache-control "no-cache" --content-type "text/html; charset=utf-8"

# Los assets tienen hash en el nombre; solo index.html necesita invalidarse
aws cloudfront create-invalidation --distribution-id "$DISTRIBUTION_ID" --paths "/index.html" "/" \
  --query Invalidation.Id --output text
paso "Frontend actualizado: https://${DOMINIO}"
