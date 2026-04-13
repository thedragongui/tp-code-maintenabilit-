# SONAR_EXECUTION - Journal d'execution

## 1) Objectif
Tracer les actions realisees pour preparer et executer l'analyse qualite (SonarQube) sur le TP RPG, avec SonarQube dans un conteneur Docker.

## 2) Environnement utilise
- OS: Windows 11
- Shell: PowerShell
- Java: OpenJDK 25.0.2 (BellSoft Liberica)
- Maven: Apache Maven 3.9.14
- Docker: 27.5.1
- Docker Compose: v2.32.4
- Projet: `0-rpg-master`

## 3) Preparation technique effectuee
- Installation de Java 25.
- Configuration `JAVA_HOME` vers:
  - `C:\Program Files\BellSoft\LibericaJDK-25`
- Installation de Maven 3.9.14.
- Validation des outils:
  - `java -version` OK
  - `mvn -v` OK

## 4) Ajustements projet realises avant analyse
- Durcissement API dans `GameController` (validation des entrees, protection reset, robustesse combat).
- Ajout de proprietes de securite dans `application.properties`.
- Renforcement des tests dans `GameControllerTest`.
- Alignement Maven/Java sur 25 dans `pom.xml` (`java.version` et `maven.compiler.release`).
- Ajout d'un compose SonarQube: `docker-compose.sonarqube.yml`.

## 5) Mise en place SonarQube en conteneur
Commandes executees:
```powershell
docker compose -f docker-compose.sonarqube.yml up -d
docker compose -f docker-compose.sonarqube.yml ps
```

Etat observe (2026-04-13):
- `sonarqube-db`: `healthy`
- `sonarqube`: `Up`
- URL: `http://localhost:9000`
- Verification API:
```powershell
Invoke-RestMethod http://localhost:9000/api/system/status
```
- Statut recu: `UP`

## 6) Verification build/tests
Commandes executees:
```powershell
mvn test
mvn clean test
```

Resultat:
- `BUILD SUCCESS`
- `Tests run: 32, Failures: 0, Errors: 0, Skipped: 0`

## 7) Statut SonarQube
- Le fichier HTML de consignes (`tp-sonarqube.html`) a ete utilise comme guide.
- Ce fichier contient des instructions pedagogiques, pas un resultat d'execution.
- SonarQube est maintenant demarre en Docker et joignable localement.
- L'analyse de projet via Maven necessite un token Sonar (a generer dans l'UI SonarQube).
- Commande d'analyse prevue:
```powershell
mvn clean verify sonar:sonar `
  -Dsonar.host.url=http://localhost:9000 `
  -Dsonar.token=<SONAR_TOKEN> `
  -Dsonar.projectKey=rpg-vulnerable `
  -Dsonar.projectName="RPG Vulnerable"
```

## 8) Prochaine etape recommandee
Lancer l'analyse SonarQube locale, puis completer ce journal avec:
- URL du serveur SonarQube local
- nom du projet/scanner
- date/heure d'analyse
- Quality Gate (OK/KO)
- nombre de Bugs, Vulnerabilities, Code Smells et hotspots
- actions correctives retenues
