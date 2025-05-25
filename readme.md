 
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
- Si le nom de la route matche "/grid/:id/production" (GET) : retourne le total de production.
- /grid/:id/consumption (GET) : retourne le total de consommation
- /grids ( aucun ID n'est fourni ) (GET) : retourne l'ID de toutes les grilles.
- /grid/:id (GET) : retourne une description JSON de la grille avec l'ID spécifié.
- Si l'ID n'est pas reconnu : erreur 404 

Dans HandlerPersons :
- /person/:id (GET) : retourne les informations d'une personne (prénom, nom, grid, capteurs possédés).
- /persons (GET) : retourne la liste des IDs de toutes les personnes.
- /person/:id (POST) : met à jour les informations d'une personne existante (prénom, nom, grid, capteurs).
- /person/:id (DELETE) : supprime la personne avec l'ID donné.
- /person (PUT) : crée une nouvelle personne avec les champs requis (prénom, nom, grid, capteurs optionnels).
- Si la route ne correspond à aucun des cas précédents : erreur 404.

Dans HandlerConsumers :
- /consumers (GET) : retourne la liste détaillée de tous les consommateurs (un objet JSON par consommateur).
- /consumer/:id (GET, via méthode privée) : retourne les informations détaillées d'un consommateur (id, nom, description, type, grid, mesures disponibles, propriétaires, puissance max, et champs spécifiques selon le type).
- Si la route ne correspond à aucun des cas précédents : erreur 404.

Dans HandlerProducer :
- /producers (GET) : retourne la liste détaillée de tous les producteurs (un objet JSON par producteur).
- /producer/:id (GET, via méthode privée) : retourne les informations détaillées d'un producteur (id, nom, description, type, grid, mesures disponibles, propriétaires, source d'énergie, et champs spécifiques selon le type).
- Si la route ne correspond à aucun des cas précédents : erreur 404

Dans SensorHandler :
- /sensor/:id (GET) : retourne les informations détaillées d'un capteur (id, nom, description, type, grid, mesures disponibles, propriétaires, et champs spécifiques selon le type).
- /sensors/:kind (GET) : retourne la liste des IDs des capteurs du type demandé (EVCharger, WindTurbine, SolarPanel).
- /sensor/:id (POST) : met à jour les informations d'un capteur existant (champs génériques et spécifiques selon le type).
- Si la route ne correspond à aucun des cas précédents : erreur 404.

Dans HandlerMeasurement :
- /measurement/:id/values (GET) : retourne la liste des valeurs (timestamp, value) d'une mesure entre deux timestamps (from, to).
- /measurement/:id (GET) : retourne les informations d'une mesure (nom, unité, capteur associé).
- Si la route ne correspond à aucun des cas précédents : erreur 404.

Dans SensorPostHandler :
- /sensor/:id (POST) : met à jour les informations d'un capteur existant (similaire à SensorHandler, mais pour les requêtes POST).
- /sensor/:id (GET, via méthode privée) : retourne les informations détaillées d'un capteur (id, nom, description, type, grid, mesures disponibles, propriétaires, et champs spécifiques selon le type).
- Si la route ne correspond à aucun des cas précédents : erreur 404.

Dans WindTurbineHandler :
- /windturbine (POST) : reçoit des données JSON d'une éolienne (id, timestamp, données de vitesse et puissance), valide et persiste les mesures, recalcule l'énergie totale produite, et retourne un statut de succès.
- Si la route ne correspond à aucun des cas précédents : erreur 404.


Dans SolarUdpHandler :
- /solarpanel (UDP, POST) : reçoit des données JSON d'un panneau solaire (id, timestamp, données de puissance et énergie), valide et persiste les mesures en créant des DataPoint et, recalcule l'énergie totale produite, et retourne un statut de succès.
- Si la route ne correspond à aucun des cas précédents : erreur 404.
Dans le frontend les Datapoins n'apparaissent pas on n'a pas pu verifier l'efficacité du code


## Détails des tests effectués (manuel, en utilisant le front-end, automatique, …) et des résultats obtenus.
Pour faire les tests, nous avons utilisé Postman, mais certains tests marchent avec Postman sans marcher avec le front-end.

Nous avons pu tester l'intégralité des GET, à la fois en tapant une requête localhost:8080, en utilisant Postman et en allant sur l'interface, car, en effet, l'interface ne fonctionnait que si une majorité des GET avaient été effectués.
Néanmoins, ce n'est pas parce qu'un GET semble fonctionner qu'il fonctionne vraiment. Par exemple, le front-end ne semble pas afficher les measurements, ce qui semble indiquer qu'il y aurait une erreur inconnue à l'intérieur.

Puis nous sommes passés aux requêtes POST qui marchent toutes en utilisant Postman. Nous avons ensuite tenté de les faire marcher sur le front-end, mais nous n'avons réussi à faire fonctionner que les POST liés aux sensors et ceux liés aux personnes.
Les POST /ingress/windturbine semblent marcher et renvoient bien un succès lorsque l'on essaie de les exécuter dans Postman, mais il est impossible de voir l'impact de ce POST directement sur le site. Cependant pour WindTurbineHandler, les résultats apparaissent bien pour datapoints et measurement sur http://localhost:8081/ mais pas dans le front-end sur http://localhost:8082/

Pour ce qui est des instructions PUT et DELETE person, nous avons réussi, encore une fois, à les faire marcher pour Postman et à constater leurs résultats sur le front-end.
Néanmoins, il semble qu'il y ait une erreur tout de même, car lorsque nous testions l'ajout d'une personne à la base de données, il y avait une page bleue avec marqué ERREUR sans autre information.
De plus, pour DELETE, dès que l'on essayait de supprimer avec Postman, on y arrivait et l'effet était visible directement dans le front-end (par l'absence des personnes que l'on a supprimées). Mais lorsqu'on essayait de le faire depuis le front-end, on obtenait cette erreur :
java.lang.Exception: 
```
java.lang.Exception: http://host.docker.internal:8080/person/ 405 METHOD_NOT_ALLOWED
```


## identifications des éventuels bogues résiduels

Plusieurs bogues persistent : on peut le voir, car nous ne pouvons pas utiliser directement les requêtes PUT et DELETE person depuis le front-end.

De plus, UDP ne semble pas fonctionner.

## remarques diverses

- Les exemples et la documentation n'étaient pas très clairs, et nous avons parfois dû utiliser l'intelligence artificielle pour trouver la solution à nos problèmes, voire même pour comprendre comment fonctionne certaines parties.




