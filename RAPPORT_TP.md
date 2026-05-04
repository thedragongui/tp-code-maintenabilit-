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

---

# Complement TP KISS - Keep It Simple, Stupid

## Objectif traite
Ajout d'une API `POST /api/tab/calculate` avec la regle metier:
- remise de 10% si la commande contient au moins un `MEAL` et au moins un `DRINK`,
- sinon aucun rabais.

## Reimplementation KISS
La logique de calcul est centralisee dans un service unique avec une methode lisible:
- somme des prix,
- verification de la condition `MEAL && DRINK`,
- application conditionnelle de la remise.

Aucun pattern Factory/Decorator/Strategy n'a ete introduit.

## Fichiers Java crees (6)
1. `src/main/java/com/taverne/kiss/controller/TabController.java`
   Expose l'endpoint HTTP et valide les donnees d'entree.
2. `src/main/java/com/taverne/kiss/service/TabCalculationService.java`
   Contient la logique metier de calcul du total.
3. `src/main/java/com/taverne/kiss/model/ItemType.java`
   Enum metier minimale pour typer les articles (`MEAL`, `DRINK`, `OTHER`).
4. `src/main/java/com/taverne/kiss/model/TabItem.java`
   Modele d'un article de commande (nom, type, prix).
5. `src/main/java/com/taverne/kiss/model/TabCalculationRequest.java`
   DTO de requete pour transporter la liste d'articles.
6. `src/main/java/com/taverne/kiss/model/TabCalculationResponse.java`
   DTO de reponse pour retourner le total calcule.

## Validation fonctionnelle
- Cas `MEAL + DRINK`: remise appliquee.
- Cas `MEAL` seul: pas de remise.
- Tests automatises ajoutes dans:
  `src/test/java/com/taverne/kiss/controller/TabControllerTest.java`

---

# Complement TP DRY - Don't Repeat Yourself

## Objectif traite
Refactoriser les paiements de guildes pour supprimer la duplication tout en conservant les 3 routes:
- `POST /api/payment/warrior`
- `POST /api/payment/mage`
- `POST /api/payment/rogue`

## Analyse de la qualite du code cible
Le code attendu par le TP est typiquement fragile quand les 3 routes embarquent chacune la formule complete:
- duplication de `price * quantity`,
- duplication du taux de taxe du Roi (5%),
- risque d'incoherence lors d'une evolution metier.

Avec ce type d'architecture, changer une regle metier impose de modifier plusieurs endroits: c'est une violation DRY.

## Refactoring applique
La logique commune a ete extraite dans un service dedie `@Service`:
- le controleur ne contient plus de formule en dur,
- les routes gardent leurs specificites via delegation au service,
- la formule `price * quantity` n'existe qu'une seule fois,
- le taux `0.05` n'existe qu'une seule fois (constante metier).

## Fichiers Java crees (3)
1. `src/main/java/com/taverne/dry/model/PaymentRequest.java`
   DTO d'entree (prix + quantite) utilise par les 3 routes.
2. `src/main/java/com/taverne/dry/service/PaymentCalculationService.java`
   Unique source de verite pour le calcul (subtotal, taxe, surcharge).
3. `src/main/java/com/taverne/dry/controller/PaymentController.java`
   Expose les 3 endpoints et delegue integralement la logique au service.

## Validation de regression
Tests automatises ajoutes:
- `src/test/java/com/taverne/dry/controller/PaymentControllerTest.java`

Cas verifies:
- Warrior (`10 x 3 + 5% + 2`) => `33.50`
- Mage (`10 x 3`) => `30.00`
- Rogue (`10 x 3 + 5%`) => `31.50`

Build global valide:
- `mvn test`
- `Tests run: 40, Failures: 0, Errors: 0, Skipped: 0`

---

# Complement TP SOLID - Les 5 principes

## Appreciation globale de la base cible
Le code cible d'origine (type "god class") concentre plusieurs responsabilites et couple le metier a des details techniques, ce qui freine l'evolutivite et les tests.
Le refactoring a ete conduit principe par principe pour rendre la base maintenable, extensible et testable.

## S - Single Responsibility Principle
Refactoring applique:
- `TavernManager` ne fait plus que l'orchestration HTTP.
- `InventoryService` gere uniquement le stock.
- `PricingService` gere uniquement le calcul de prix.
- `OrderService` gere l'enregistrement logique de commande et la notification.

Une classe = une raison principale de changer.

## O - Open / Closed Principle
Refactoring applique:
- creation de l'abstraction `PricingRule` avec `apply(double baseTotal)`.
- implementations separees:
  - `KingTaxRule` (+5%)
  - `NightSurchargeRule` (+10% apres 22h)
  - `WeekendDiscountRule` (-5% le week-end)
- `PricingService` itere sur `List<PricingRule>` sans connaitre les classes concretes.

Ajout d'une regle nouvelle sans modification de `PricingService`: OCP respecte.

## L - Liskov Substitution Principle
Refactoring applique:
- `ConsumableItem#isSafeToConsume()` retourne `true` par defaut.
- `PoisonousDrink extends ConsumableItem` retourne `false` sans exception.
- verification par code client sur `List<ConsumableItem>` mixte.

Substitution possible sans casser le comportement attendu.

## I - Interface Segregation Principle
Refactoring applique:
- interface monolithique `IItemActions` retiree.
- interfaces ciblees:
  - `ICookable` (`cook`, `roast`)
  - `IPourable` (`pourIntoMug`)
- classes specialisees:
  - `Bread implements ICookable`
  - `Ale implements IPourable`

Chaque classe depend uniquement des methodes qui la concernent.

## D - Dependency Inversion Principle
Refactoring applique:
- abstraction `INotificationRepository`.
- implementations:
  - `InMemoryNotificationRepository`
  - `SqlNotificationRepository`
- `OrderService` depend de l'interface et la recoit par injection constructeur.
- aucun `new XyzRepository()` dans `OrderService`.

Le module metier depend d'une abstraction et non d'un detail de persistance.

## Fichiers Java ajoutes (SOLID)
- `src/main/java/com/taverne/solid/controller/TavernManager.java`
- `src/main/java/com/taverne/solid/service/OrderService.java`
- `src/main/java/com/taverne/solid/service/InventoryService.java`
- `src/main/java/com/taverne/solid/service/PricingService.java`
- `src/main/java/com/taverne/solid/pricing/PricingRule.java`
- `src/main/java/com/taverne/solid/pricing/KingTaxRule.java`
- `src/main/java/com/taverne/solid/pricing/NightSurchargeRule.java`
- `src/main/java/com/taverne/solid/pricing/WeekendDiscountRule.java`
- `src/main/java/com/taverne/solid/repository/INotificationRepository.java`
- `src/main/java/com/taverne/solid/repository/InMemoryNotificationRepository.java`
- `src/main/java/com/taverne/solid/repository/SqlNotificationRepository.java`
- `src/main/java/com/taverne/solid/interfaces/ICookable.java`
- `src/main/java/com/taverne/solid/interfaces/IPourable.java`
- `src/main/java/com/taverne/solid/model/OrderLine.java`
- `src/main/java/com/taverne/solid/model/OrderRequest.java`
- `src/main/java/com/taverne/solid/model/OrderResponse.java`
- `src/main/java/com/taverne/solid/model/ConsumableItem.java`
- `src/main/java/com/taverne/solid/model/PoisonousDrink.java`
- `src/main/java/com/taverne/solid/model/Bread.java`
- `src/main/java/com/taverne/solid/model/Ale.java`
- `src/main/java/com/taverne/solid/config/SolidClockConfiguration.java`

## Tests ajoutes (SOLID)
- `src/test/java/com/taverne/solid/controller/TavernManagerTest.java`
- `src/test/java/com/taverne/solid/pricing/PricingRulesTest.java`
- `src/test/java/com/taverne/solid/model/SolidContractsTest.java`

## Validation
- `mvn test` execute avec succes apres refactoring SOLID.
- resultat global actuel: `Tests run: 48, Failures: 0, Errors: 0, Skipped: 0`.
