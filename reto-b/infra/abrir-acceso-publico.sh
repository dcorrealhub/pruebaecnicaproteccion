#!/usr/bin/env bash
# Abre PostgreSQL (5432) y SSH (22) de la EC2 hacia internet e instala la llave pública
# del key pair indicado en ec2-user. Pensado para conectarse desde DataGrip / ssh en la demo.
#
# ADVERTENCIA: con CIDR=0.0.0.0/0 los puertos quedan expuestos a escaneos y fuerza bruta.
# Para limitarlo a una IP: CIDR=200.189.27.110/32 bash abrir-acceso-publico.sh
source "$(dirname "$0")/comun.sh"
: "${INSTANCE_ID:?Primero ejecuta deploy.sh}"
: "${SG_ID:?Primero ejecuta deploy.sh}"

CIDR="${CIDR:-0.0.0.0/0}"
KEY_PAIR="${KEY_PAIR:-access}"

paso "Security group $SG_ID: 22 y 5432 desde $CIDR"
for PUERTO in 22 5432; do
  aws ec2 authorize-security-group-ingress --group-id "$SG_ID" --ip-permissions \
    "IpProtocol=tcp,FromPort=${PUERTO},ToPort=${PUERTO},IpRanges=[{CidrIp=${CIDR},Description=acceso-publico-demo}]" \
    >/dev/null 2>&1 || echo "  (la regla del puerto $PUERTO ya existía)"
done

paso "Llave pública del key pair '$KEY_PAIR'"
LLAVE="$(aws ec2 describe-key-pairs --key-names "$KEY_PAIR" --include-public-key \
  --query 'KeyPairs[0].PublicKey' --output text | tr -d '\r\n')"

paso "Configurando la instancia vía SSM (llave SSH + PostgreSQL escuchando externamente)"
cat > "$INFRA_DIR/.ssm-params.json" <<EOF
{"commands":[
  "set -euo pipefail",
  "install -d -m 700 -o ec2-user -g ec2-user /home/ec2-user/.ssh",
  "touch /home/ec2-user/.ssh/authorized_keys",
  "grep -qxF '${LLAVE}' /home/ec2-user/.ssh/authorized_keys || echo '${LLAVE}' >> /home/ec2-user/.ssh/authorized_keys",
  "chown ec2-user:ec2-user /home/ec2-user/.ssh/authorized_keys && chmod 600 /home/ec2-user/.ssh/authorized_keys",
  "if docker port reto-b-db 5432 | grep -q '^127.0.0.1'; then DBP=\$(grep '^DB_PASSWORD=' /etc/reto-b/env | cut -d= -f2); docker stop reto-b-db >/dev/null && docker rm reto-b-db >/dev/null; docker run -d --name reto-b-db --restart unless-stopped -e POSTGRES_DB=proteccion_reto -e POSTGRES_PASSWORD=\$DBP -p 5432:5432 -v reto-b-pgdata:/var/lib/postgresql/data postgres:15-alpine >/dev/null; fi",
  "for i in \$(seq 1 30); do docker exec reto-b-db pg_isready -U postgres >/dev/null 2>&1 && break; sleep 1; done",
  "systemctl restart reto-b",
  "docker port reto-b-db 5432"
]}
EOF
COMANDO="$(aws ssm send-command --instance-ids "$INSTANCE_ID" --document-name AWS-RunShellScript \
  --comment "reto-b acceso publico" --parameters "file://$(ruta_cli "$INFRA_DIR/.ssm-params.json")" \
  --query Command.CommandId --output text)"
rm -f "$INFRA_DIR/.ssm-params.json"
aws ssm wait command-executed --command-id "$COMANDO" --instance-id "$INSTANCE_ID" || true
aws ssm get-command-invocation --command-id "$COMANDO" --instance-id "$INSTANCE_ID" \
  --query '[Status,StandardOutputContent,StandardErrorContent]' --output text

EC2_DNS="$(aws ec2 describe-instances --instance-ids "$INSTANCE_ID" \
  --query 'Reservations[0].Instances[0].PublicDnsName' --output text)"
paso "Listo. SSH: ssh -i ${KEY_PAIR}.pem ec2-user@${EC2_DNS}  |  PostgreSQL: ${EC2_DNS}:5432 / proteccion_reto"
