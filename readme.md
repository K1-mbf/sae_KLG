
## Pre-requis

- un JRE pour faire tourner gradle
- Docker : guide d'installation [ici](https://docs.docker.com/engine/install/)

## Mise en place

- Placez vous dans ce répertoire et exécutez la commande `docker compose up -d` pour lancer le serveur postgresql
- Lancez le projet avec `./gradlew.bat run` (utilisez `gradlew` sur macOS / linux)

Le backend est accessible sur le port `8080`, le frontend est accessible [ici](http://localhost:8082).
Une interface web pour administrer la base de données est accessible [ici](http://localhost:80801), sélectionnez `PostgreSQL` comme système, `db` comme serveur et `test` comme utilisateur/mot de passe/base de données. 

## Identification du groupe
Kéwan MARBOEUF, Gaëtan MARCHAND, Lianne SOO

## Détails des routes implémentées (éventuellement routes non-implémentées)
Dans HandlerGrid : 
- Si le nom de la route matche "/grid/:id/production" : retourne le total de production.
- /grid/:id/consumption : retourne le total de consommation
- /grids ( aucun ID n'est fourni ) : retourne l'ID de toutes les grilles.
- /grid/:id : retourne une description JSON de la grille avec l'ID spécifié.
- Si l'ID n'est pas reconnu : erreur 404 

Dans HandlerPersons :
- /person/:id (GET) : retourne les informations d'une personne (prénom, nom, grid, capteurs possédés).
- /persons (GET) : retourne la liste des IDs de toutes les personnes.
- /person/:id (POST) : met à jour les informations d'une personne existante (prénom, nom, grid, capteurs).
- /person/:id (DELETE) : supprime la personne avec l'ID donné.
- /person (PUT) : crée une nouvelle personne avec les champs requis (prénom, nom, grid, capteurs optionnels).
- Si la route ne correspond à aucun des cas précédents : erreur 404.

Dans HandlerConsumers:
/consumers (GET) : retourne la liste détaillée de tous les consommateurs (un objet JSON par consommateur).
/consumer/:id (GET, via méthode privée) : retourne les informations détaillées d'un consommateur (id, nom, description, type, grid, mesures disponibles, propriétaires, puissance max, et champs spécifiques selon le type).
Si la route ne correspond à aucun des cas précédents : erreur 404.



## Détails des tests effectués (manuel, en utilisant le front-end, automatique, …) et des résultats obtenus.

identifications des éventuels bogues résiduels
remarques diverses