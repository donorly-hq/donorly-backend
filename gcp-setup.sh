#!/usr/bin/env bash
# =============================================================================
# gcp-setup.sh  —  One-time GCP bootstrap for Donorly
# Run once from your local machine after: gcloud auth login
#
# Usage:
#   chmod +x gcp-setup.sh
#   ./gcp-setup.sh
# =============================================================================

set -euo pipefail

PROJECT_ID="donorly-app-501321"
REGION="us-central1"
REPO="donorly"

echo ">>> Setting active project..."
gcloud config set project "$PROJECT_ID"

echo ">>> Enabling required APIs..."
gcloud services enable \
  run.googleapis.com \
  cloudbuild.googleapis.com \
  artifactregistry.googleapis.com \
  secretmanager.googleapis.com \
  --quiet

echo ">>> Creating Artifact Registry repository..."
gcloud artifacts repositories create "$REPO" \
  --repository-format=docker \
  --location="$REGION" \
  --description="Donorly container images" \
  --quiet || echo "Repository already exists, skipping."

echo ">>> Creating secrets in Secret Manager..."

# JWT Secret
printf '%s' "995wLLabUgNxMxMTU2X+5yy7kTQww5rzZiuwbF1MSDKuu+30GqOWS+cENWRFkt2M" \
  | gcloud secrets create donorly-jwt-secret \
      --data-file=- --replication-policy=automatic --quiet \
  || echo "donorly-jwt-secret already exists."

# DB Password
printf '%s' "YRpGrobvIsDMnKhnDFNonPlprHfAIYAH" \
  | gcloud secrets create donorly-db-password \
      --data-file=- --replication-policy=automatic --quiet \
  || echo "donorly-db-password already exists."

# Mail (App) Password
printf '%s' "ikxx latr vjem zqry" \
  | gcloud secrets create donorly-mail-password \
      --data-file=- --replication-policy=automatic --quiet \
  || echo "donorly-mail-password already exists."

echo ">>> Granting Cloud Build access to secrets and Cloud Run..."
PROJECT_NUMBER=$(gcloud projects describe "$PROJECT_ID" --format="value(projectNumber)")
CB_SA="${PROJECT_NUMBER}@cloudbuild.gserviceaccount.com"

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:${CB_SA}" \
  --role="roles/run.admin" --quiet

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:${CB_SA}" \
  --role="roles/secretmanager.secretAccessor" --quiet

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:${CB_SA}" \
  --role="roles/iam.serviceAccountUser" --quiet

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:${CB_SA}" \
  --role="roles/artifactregistry.writer" --quiet

echo ""
echo "============================================================"
echo " GCP setup complete for project: $PROJECT_ID"
echo "============================================================"
echo ""
echo "NEXT STEPS:"
echo ""
echo "1. Connect your GitHub repos to Cloud Build:"
echo "   https://console.cloud.google.com/cloud-build/triggers?project=$PROJECT_ID"
echo ""
echo "   Backend trigger:"
echo "     Repo: donorly-backend"
echo "     Branch: main"
echo "     Build config: cloudbuild.yaml"
echo ""
echo "   Frontend trigger:"
echo "     Repo: donorly-portal"
echo "     Branch: main"
echo "     Build config: cloudbuild.yaml"
echo ""
echo "2. After the FIRST backend deploy, get its URL:"
echo "   gcloud run services describe donorly-backend --region=$REGION --format='value(status.url)'"
echo ""
echo "3. Update _API_BASE in donorly-portal/cloudbuild.yaml with that URL,"
echo "   then push to trigger the frontend deploy."
echo "============================================================"
