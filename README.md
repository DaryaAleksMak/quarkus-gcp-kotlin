# quarkus-gcp-demo

This project uses Quarkus, the Supersonic Subatomic Java Framework.

deploy on GCP

docker buildx build --platform linux/amd64 -f src/main/docker/Dockerfile -t quarkus/quarkus-gcp-kotlin-jvm .
docker tag quarkus/quarkus-gcp-kotlin-jvm:latest us-docker.pkg.dev/quarkus-gcp-kotlin/quarkus-service-app/quarkus-gcp-kotlin-jvm
docker push us-docker.pkg.dev/quarkus-gcp-kotlin/quarkus-service-app/quarkus-gcp-kotlin-jvm
gcloud run deploy --image us-docker.pkg.dev/quarkus-gcp-kotlin/quarkus-service-app/quarkus-gcp-kotlin-jvm

url:


https://quarkus-gcp-demo-service-4c7impmuda-uc.a.run.app