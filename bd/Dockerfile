FROM postgres:16

# Variables par défaut (écrasées par le docker-compose / .env si besoin)
ENV POSTGRES_USER=crypto_user
ENV POSTGRES_PASSWORD=crypto_pass
ENV POSTGRES_DB=crypto_db

# Script d'init : création des tables, données de base, etc.
COPY init.sql /docker-entrypoint-initdb.d/

# Le reste est géré par l'image officielle
