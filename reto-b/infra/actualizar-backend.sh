#!/usr/bin/env bash
# Compila el backend, sube el jar y reinicia el servicio en la EC2 vía SSM (sin SSH).
source "$(dirname "$0")/comun.sh"
: "${INSTANCE_ID:?Primero ejecuta deploy.sh}"

paso "Compilando y probando backend"
(cd "$RETO_B_DIR/backend" && env -u MSYS_NO_PATHCONV ./mvnw -q -B package)
JAR="$(ls "$RETO_B_DIR"/backend/target/reto-b-*.jar | grep -v original | head -1)"
aws s3 cp "$(ruta_cli "$JAR")" "s3://$BUCKET_ARTEFACTOS/reto-b.jar" --only-show-errors

paso "Reiniciando el servicio en $INSTANCE_ID"
COMANDO="$(aws ssm send-command --instance-ids "$INSTANCE_ID" --document-name AWS-RunShellScript \
  --comment "reto-b actualizar backend" --parameters 'commands=["/opt/reto-b/actualizar.sh"]' \
  --query Command.CommandId --output text)"
aws ssm wait command-executed --command-id "$COMANDO" --instance-id "$INSTANCE_ID"
aws ssm get-command-invocation --command-id "$COMANDO" --instance-id "$INSTANCE_ID" --query Status --output text
paso "Backend actualizado: https://${DOMINIO}/api/aportes"
