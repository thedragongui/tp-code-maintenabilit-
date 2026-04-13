# Rapport TP - RPG Vulnerable

## Contexte
Le README demandait d'analyser le code source, d'identifier les faiblesses (securite/robustesse), puis de proposer et appliquer des ameliorations concretes.

## Ce qui a ete fait

### 1) Audit du backend
- Lecture du controleur principal `GameController`.
- Identification des zones a risque:
  - validation d'entrees quasi absente,
  - endpoint de reset accessible sans protection,
  - logique PvP permissive (auto-ciblage possible),
  - recursion pour la montee de niveau,
  - CORS ouvert a toutes les origines.

### 2) Durcissement de l'API
- Ajout de validations explicites:
  - `playerId` contraint par regex (`[A-Za-z0-9_-]{3,64}`),
  - `mode` limite a `SOLO`, `COOP`, `PVP`,
  - `itemIndex` doit etre un entier `>= 0`.
- Retour d'erreurs HTTP propres (`400`) au lieu d'erreurs serveur implicites.
- Renforcement du PvP:
  - `targetId` obligatoire en mode `PVP`,
  - auto-ciblage interdit,
  - verification de l'existence de la cible.
- Protection des HP contre les valeurs negatives (`GREATEST(0, hp - degats)`).

### 3) Protection operation sensible
- Endpoint `POST /api/v1/hero/reset` protege par token admin optionnel:
  - header `X-ADMIN-TOKEN`,
  - en cas de token configure et invalide: `403 Forbidden`.

### 4) Robustesse interne
- Initialisation DB rendue thread-safe (`checkDb` synchronise).
- Remplacement de la recursion de level-up par une boucle bornee (`MAX_LEVEL_UPS_PER_REFRESH`) pour eviter les risques de stack overflow.
- Gestion plus defensive des ennemis solo manquants (recreation si necessaire).

### 5) Configuration ajoutee
Dans `application.properties`:
- `app.cors.allowed-origin-patterns=http://localhost:*`
- `app.security.reset-token=`

Ces valeurs permettent un comportement local simple, tout en autorisant un durcissement via profile/env en production.

### 6) Tests de non-regression
Le fichier de tests a ete mis a jour pour couvrir:
- ID joueur invalide,
- PvP sans cible,
- PvP auto-cible,
- index d'objet negatif,
- reset sans token (403),
- reset avec token valide.

## Fichiers modifies
- `src/main/java/com/epsi/rpg/controller/GameController.java`
- `src/main/resources/application.properties`
- `src/test/java/com/epsi/rpg/controller/GameControllerTest.java`
- `pom.xml`

## Problemes regles (explication attendue en soutenance)

### Probleme 1 - Entrees utilisateur non validees
- **Avant :** l'API acceptait des valeurs invalides (`playerId` vide, `mode` incorrect, `itemIndex` negatif), ce qui pouvait produire des comportements incoherents ou des erreurs serveur.
- **Correction :** ajout de validations centralisees (`validatePlayerId`, `normalizeMode`, `requireNonNegativeInt`) et retour HTTP `400` explicite.
- **Impact :** API plus robuste, erreurs metier gerables cote front, reduction des crashs.

### Probleme 2 - Reset global non protege
- **Avant :** `POST /api/v1/hero/reset` etait appelable sans controle, donc destruction possible de l'etat de jeu par n'importe quel client.
- **Correction :** protection par token admin optionnel via header `X-ADMIN-TOKEN` (`403` si invalide quand active).
- **Impact :** operation destructive controlee, surface d'attaque reduite.

### Probleme 3 - Combat PvP insuffisamment contraint
- **Avant :** en PvP, la cible pouvait etre absente ou invalide, et l'auto-ciblage etait possible.
- **Correction :** `targetId` obligatoire en `PVP`, interdiction d'attaquer son propre joueur, verification de l'existence de la cible.
- **Impact :** logique de combat coherente et sans cas aberrants.

### Probleme 4 - Valeurs de HP non bornees
- **Avant :** les degats pouvaient pousser les HP sous zero.
- **Correction :** mise a jour SQL bornee (`GREATEST(0, hp - degats)`).
- **Impact :** etat de jeu valide en permanence.

### Probleme 5 - Montee de niveau recursive
- **Avant :** la progression utilisait une recursion (`internalGetState`), risque de profondeur importante avec beaucoup d'XP.
- **Correction :** remplacement par une boucle bornee (`MAX_LEVEL_UPS_PER_REFRESH`).
- **Impact :** comportement deterministe, risque de stack overflow elimine.

### Probleme 6 - CORS trop permissif
- **Avant :** `@CrossOrigin(origins="*")`.
- **Correction :** passage a une origine/pattern configurable (`app.cors.allowed-origin-patterns`), valeur par defaut limitee au local.
- **Impact :** meilleure hygiene securite pour un deploiement reel.

### Probleme 7 - Incoherence environnement de build Java
- **Avant :** Java 25 installe, mais compilation Maven observee en `release 17`.
- **Correction :** ajout de `java.version=25` et `maven.compiler.release=25` dans `pom.xml`.
- **Impact :** build aligne sur la version cible, compilation/test coherents.

### Probleme 8 - SonarQube non conteneurise
- **Avant :** aucune stack Docker SonarQube versionnee dans le projet.
- **Correction :** ajout de `docker-compose.sonarqube.yml` (SonarQube + PostgreSQL + volumes persistants).
- **Impact :** environnement Sonar reproductible localement et conforme a la consigne "SonarQube dans un conteneur Docker".

### Validation des corrections
- Suite de tests executee avec succes:
  - `mvn clean test`
  - `Tests run: 32, Failures: 0, Errors: 0, Skipped: 0`

## Limites connues
- L'analyse SonarQube n'est pas encore documentee avec des mesures finales (Quality Gate, Bugs, Vulnerabilities, Code Smells) tant que le scan Maven avec token n'est pas lance.

## Conclusion
Le TP a ete traite avec une approche "audit + correction": reduction de la surface d'attaque, validation stricte des entrees, protection des operations sensibles, et ajout de tests cibles pour limiter les regressions.
