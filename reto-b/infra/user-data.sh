#!/bin/bash
# Arranque de la instancia EC2 (Amazon Linux 2023).
# Instala Java 21 y Docker, levanta PostgreSQL 15 en un contenedor (solo escucha en localhost)
# y deja el backend corriendo como servicio systemd en el puerto 8080.
# __ARTIFACT_BUCKET__ y __REGION__ los reemplaza deploy.sh.
set -euxo pipefail

ARTIFACT_BUCKET="__ARTIFACT_BUCKET__"
REGION="__REGION__"

dnf install -y java-21-amazon-corretto-headless docker
systemctl enable --now docker

# Credenciales de la base: se generan en la instancia y nunca salen de ella.
# El archivo es solo legible por root; systemd lo lee antes de bajar privilegios.
install -d -m 700 /etc/reto-b
if [ ! -f /etc/reto-b/env ]; then
  DB_PASSWORD_GENERADO="$(openssl rand -hex 24)"
  cat > /etc/reto-b/env <<EOF
DB_URL=jdbc:postgresql://127.0.0.1:5432/proteccion_reto
DB_USER=postgres
DB_PASSWORD=${DB_PASSWORD_GENERADO}
EOF
  chmod 600 /etc/reto-b/env
fi
DB_PASSWORD="$(grep '^DB_PASSWORD=' /etc/reto-b/env | cut -d= -f2)"

if ! docker ps -a --format '{{.Names}}' | grep -qx reto-b-db; then
  docker run -d --name reto-b-db --restart unless-stopped \
    -e POSTGRES_DB=proteccion_reto \
    -e POSTGRES_PASSWORD="${DB_PASSWORD}" \
    -p 127.0.0.1:5432:5432 \
    -v reto-b-pgdata:/var/lib/postgresql/data \
    postgres:15-alpine
fi

id retob &>/dev/null || useradd --system --no-create-home --shell /sbin/nologin retob
install -d -o retob -g retob /opt/reto-b

# Script de actualización: lo usa también actualizar-backend.sh vía SSM
cat > /opt/reto-b/actualizar.sh <<EOF
#!/bin/bash
set -euo pipefail
aws s3 cp --region ${REGION} s3://${ARTIFACT_BUCKET}/reto-b.jar /opt/reto-b/app.jar.nuevo
chown retob:retob /opt/reto-b/app.jar.nuevo
mv /opt/reto-b/app.jar.nuevo /opt/reto-b/app.jar
systemctl restart reto-b
EOF
chmod 750 /opt/reto-b/actualizar.sh

cat > /etc/systemd/system/reto-b.service <<'EOF'
[Unit]
Description=Reto B - API de aportes voluntarios
After=docker.service network-online.target
Wants=network-online.target

[Service]
User=retob
WorkingDirectory=/opt/reto-b
EnvironmentFile=/etc/reto-b/env
ExecStart=/usr/bin/java -Xms256m -Xmx768m -jar /opt/reto-b/app.jar
# Si PostgreSQL aún no está listo en el primer arranque, systemd reintenta
Restart=always
RestartSec=10
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable reto-b
/opt/reto-b/actualizar.sh
