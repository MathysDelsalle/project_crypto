Requis pour lancer le projet :

Docker version 28.5.1
minikube version: v1.37.0
Client Version: v1.34.3
Kustomize Version: v5.7.1
Apache Maven 3.8.7
Java version: 17.0.17
node v20.19.6
npm 10.8.2
GNU Make 4.3
curl 8.5.0 
jq-1.7
git version 2.43.0


Pour lancer le projet la premiere fois: 

make firststart

pour relancer le projet:

make start

pour afficher les conteneurs et voir leur état :

make status 

et pour voir les logs :

make logs-api
make logs-front
make logs-collector

pour le tests: 
make test
make perf

