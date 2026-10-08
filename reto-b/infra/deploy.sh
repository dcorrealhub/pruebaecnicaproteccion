#!/usr/bin/env bash
# Despliega reto-b en AWS:
#   CloudFront ─┬─ /*     → S3 privado (frontend, acceso solo vía OAC)
#               └─ /api/* → EC2 :8080 (backend + PostgreSQL en Docker; SG solo admite CloudFront)
# Es reanudable: cada recurso creado queda en infra/estado.env y no se vuelve a crear.
source "$(dirname "$0")/comun.sh"

# Políticas administradas de CloudFront
CACHE_OPTIMIZADO=658327ea-f89d-4fab-a63d-7e88639e58f6
CACHE_DESACTIVADO=4135ea2d-6df8-44a3-9df3-4b5a84be39ad
REQUEST_TODO_MENOS_HOST=b689b0a8-53d0-40ab-baf2-68738e2966ac

TMP="$INFRA_DIR/.tmp"
mkdir -p "$TMP"

# ------------------------------------------------------------------ build
paso "Compilando backend (las pruebas ya corren aparte con ./mvnw test)"
(cd "$RETO_B_DIR/backend" && env -u MSYS_NO_PATHCONV ./mvnw -q -B -DskipTests package)
JAR="$(ls "$RETO_B_DIR"/backend/target/reto-b-*.jar | grep -v original | head -1)"

paso "Compilando frontend"
(cd "$RETO_B_DIR/frontend" && { [ -d node_modules ] || npm ci --no-audit --no-fund --silent; } && env -u MSYS_NO_PATHCONV npm run build --silent)

# ------------------------------------------------------- bucket artefactos
crear_bucket_privado() {
  local bucket="$1"
  if ! aws s3api head-bucket --bucket "$bucket" 2>/dev/null; then
    aws s3api create-bucket --bucket "$bucket" --create-bucket-configuration LocationConstraint="$REGION" >/dev/null
  fi
  aws s3api put-public-access-block --bucket "$bucket" --public-access-block-configuration \
    BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
  aws s3api put-bucket-tagging --bucket "$bucket" --tagging "TagSet=[{Key=Project,Value=${PROYECTO}}]"
}

paso "Bucket de artefactos: $BUCKET_ARTEFACTOS"
crear_bucket_privado "$BUCKET_ARTEFACTOS"
aws s3 cp "$(ruta_cli "$JAR")" "s3://$BUCKET_ARTEFACTOS/reto-b.jar" --only-show-errors

# ---------------------------------------------------------------- IAM EC2
paso "Rol IAM de la instancia: $ROL_EC2 (SSM + lectura del jar)"
if ! aws iam get-role --role-name "$ROL_EC2" >/dev/null 2>&1; then
  aws iam create-role --role-name "$ROL_EC2" --tags Key=Project,Value="$PROYECTO" \
    --assume-role-policy-document '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"ec2.amazonaws.com"},"Action":"sts:AssumeRole"}]}' >/dev/null
  aws iam attach-role-policy --role-name "$ROL_EC2" \
    --policy-arn arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore
  aws iam put-role-policy --role-name "$ROL_EC2" --policy-name leer-artefactos --policy-document \
    "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Action\":\"s3:GetObject\",\"Resource\":\"arn:aws:s3:::${BUCKET_ARTEFACTOS}/*\"}]}"
  aws iam create-instance-profile --instance-profile-name "$ROL_EC2" >/dev/null
  aws iam add-role-to-instance-profile --instance-profile-name "$ROL_EC2" --role-name "$ROL_EC2"
  echo "Esperando propagación de IAM..."
  sleep 15
fi

# ---------------------------------------------------------- security group
if [ -z "${SG_ID:-}" ]; then
  paso "Security group (8080 solo desde CloudFront)"
  VPC_ID="$(aws ec2 describe-vpcs --filters Name=is-default,Values=true --query 'Vpcs[0].VpcId' --output text)"
  PL_CLOUDFRONT="$(aws ec2 describe-managed-prefix-lists \
    --filters Name=prefix-list-name,Values=com.amazonaws.global.cloudfront.origin-facing \
    --query 'PrefixLists[0].PrefixListId' --output text)"
  guardar SG_ID "$(aws ec2 create-security-group --group-name "${PROYECTO}-backend" \
    --description "reto-b backend: 8080 solo desde CloudFront" --vpc-id "$VPC_ID" \
    --tag-specifications "ResourceType=security-group,Tags=[${TAGS_EC2}]" \
    --query GroupId --output text)"
  aws ec2 authorize-security-group-ingress --group-id "$SG_ID" --ip-permissions \
    "IpProtocol=tcp,FromPort=8080,ToPort=8080,PrefixListIds=[{PrefixListId=${PL_CLOUDFRONT},Description=CloudFront}]" >/dev/null
fi

# ---------------------------------------------------------------- EC2
if [ -z "${INSTANCE_ID:-}" ]; then
  paso "Instancia EC2 t3.small (Amazon Linux 2023)"
  AMI="$(aws ssm get-parameter --name /aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64 \
    --query Parameter.Value --output text)"
  SUBNET="$(aws ec2 describe-subnets --filters Name=default-for-az,Values=true \
    --query 'sort_by(Subnets,&AvailabilityZone)[0].SubnetId' --output text)"
  sed -e "s|__ARTIFACT_BUCKET__|${BUCKET_ARTEFACTOS}|" -e "s|__REGION__|${REGION}|" \
    "$INFRA_DIR/user-data.sh" > "$TMP/user-data.sh"
  guardar INSTANCE_ID "$(aws ec2 run-instances \
    --image-id "$AMI" --instance-type t3.small \
    --subnet-id "$SUBNET" --security-group-ids "$SG_ID" --associate-public-ip-address \
    --iam-instance-profile Name="$ROL_EC2" \
    --metadata-options HttpTokens=required,HttpEndpoint=enabled \
    --block-device-mappings 'DeviceName=/dev/xvda,Ebs={VolumeSize=20,VolumeType=gp3,Encrypted=true}' \
    --user-data "file://$(ruta_cli "$TMP/user-data.sh")" \
    --tag-specifications "ResourceType=instance,Tags=[${TAGS_EC2},{Key=Name,Value=${PROYECTO}-backend}]" \
                         "ResourceType=volume,Tags=[${TAGS_EC2}]" \
    --query 'Instances[0].InstanceId' --output text)"
fi
aws ec2 wait instance-running --instance-ids "$INSTANCE_ID"
EC2_DNS="$(aws ec2 describe-instances --instance-ids "$INSTANCE_ID" \
  --query 'Reservations[0].Instances[0].PublicDnsName' --output text)"
echo "Instancia $INSTANCE_ID en $EC2_DNS"

# ------------------------------------------------------------ frontend S3
paso "Bucket del frontend: $BUCKET_FRONT"
crear_bucket_privado "$BUCKET_FRONT"
DIST_DIR="$RETO_B_DIR/frontend/dist"
# Los assets llevan hash en el nombre: se cachean un año. index.html siempre se revalida.
aws s3 sync "$(ruta_cli "$DIST_DIR/assets")" "s3://$BUCKET_FRONT/assets" --delete --only-show-errors \
  --cache-control "public,max-age=31536000,immutable"
aws s3 cp "$(ruta_cli "$DIST_DIR/index.html")" "s3://$BUCKET_FRONT/index.html" --only-show-errors \
  --cache-control "no-cache" --content-type "text/html; charset=utf-8"

# ------------------------------------------------------------- CloudFront
if [ -z "${OAC_ID:-}" ]; then
  paso "Origin Access Control"
  guardar OAC_ID "$(aws cloudfront create-origin-access-control --origin-access-control-config \
    "Name=${PROYECTO}-${REGION}-oac,Description=reto-b frontend,SigningProtocol=sigv4,SigningBehavior=always,OriginAccessControlOriginType=s3" \
    --query OriginAccessControl.Id --output text)"
fi

if [ -z "${DISTRIBUTION_ID:-}" ]; then
  paso "Distribución CloudFront"
  cat > "$TMP/distribucion.json" <<EOF
{
  "CallerReference": "${PROYECTO}-$(date +%s)",
  "Comment": "reto-b aportes voluntarios",
  "Enabled": true,
  "DefaultRootObject": "index.html",
  "PriceClass": "PriceClass_100",
  "HttpVersion": "http2and3",
  "Origins": {
    "Quantity": 2,
    "Items": [
      {
        "Id": "s3-frontend",
        "DomainName": "${BUCKET_FRONT}.s3.${REGION}.amazonaws.com",
        "OriginAccessControlId": "${OAC_ID}",
        "S3OriginConfig": { "OriginAccessIdentity": "" }
      },
      {
        "Id": "ec2-backend",
        "DomainName": "${EC2_DNS}",
        "CustomOriginConfig": {
          "HTTPPort": 8080,
          "HTTPSPort": 443,
          "OriginProtocolPolicy": "http-only",
          "OriginSslProtocols": { "Quantity": 1, "Items": ["TLSv1.2"] },
          "OriginReadTimeout": 30,
          "OriginKeepaliveTimeout": 5
        }
      }
    ]
  },
  "DefaultCacheBehavior": {
    "TargetOriginId": "s3-frontend",
    "ViewerProtocolPolicy": "redirect-to-https",
    "CachePolicyId": "${CACHE_OPTIMIZADO}",
    "Compress": true,
    "AllowedMethods": {
      "Quantity": 2, "Items": ["GET", "HEAD"],
      "CachedMethods": { "Quantity": 2, "Items": ["GET", "HEAD"] }
    }
  },
  "CacheBehaviors": {
    "Quantity": 1,
    "Items": [
      {
        "PathPattern": "/api/*",
        "TargetOriginId": "ec2-backend",
        "ViewerProtocolPolicy": "https-only",
        "CachePolicyId": "${CACHE_DESACTIVADO}",
        "OriginRequestPolicyId": "${REQUEST_TODO_MENOS_HOST}",
        "Compress": true,
        "AllowedMethods": {
          "Quantity": 7, "Items": ["GET", "HEAD", "OPTIONS", "PUT", "POST", "PATCH", "DELETE"],
          "CachedMethods": { "Quantity": 2, "Items": ["GET", "HEAD"] }
        }
      }
    ]
  }
}
EOF
  guardar DISTRIBUTION_ID "$(aws cloudfront create-distribution \
    --distribution-config "file://$(ruta_cli "$TMP/distribucion.json")" \
    --query Distribution.Id --output text)"
  aws cloudfront tag-resource --resource "arn:aws:cloudfront::${CUENTA}:distribution/${DISTRIBUTION_ID}" \
    --tags "Items=[{Key=Project,Value=${PROYECTO}}]"
fi

paso "Política del bucket: solo esta distribución puede leer el frontend"
aws s3api put-bucket-policy --bucket "$BUCKET_FRONT" --policy "{
  \"Version\": \"2012-10-17\",
  \"Statement\": [{
    \"Sid\": \"SoloCloudFront\",
    \"Effect\": \"Allow\",
    \"Principal\": {\"Service\": \"cloudfront.amazonaws.com\"},
    \"Action\": \"s3:GetObject\",
    \"Resource\": \"arn:aws:s3:::${BUCKET_FRONT}/*\",
    \"Condition\": {\"StringEquals\": {\"AWS:SourceArn\": \"arn:aws:cloudfront::${CUENTA}:distribution/${DISTRIBUTION_ID}\"}}
  }]
}"

DOMINIO="$(aws cloudfront get-distribution --id "$DISTRIBUTION_ID" --query Distribution.DomainName --output text)"
grep -q '^DOMINIO=' "$ESTADO" 2>/dev/null || guardar DOMINIO "$DOMINIO"

paso "Esperando que CloudFront termine de desplegar (suele tardar 5-10 min)"
aws cloudfront wait distribution-deployed --id "$DISTRIBUTION_ID"

rm -rf "$TMP"
paso "Listo: https://${DOMINIO}"
