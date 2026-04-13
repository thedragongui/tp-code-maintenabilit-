# RPG Vulnérable

Ce projet est une application de jeu de rôle (RPG) simplifiée, conçue pour illustrer des concepts de sécurité et de performance dans le développement d'applications web.

## Description

L'application permet aux joueurs de combattre des monstres, de gagner de l'expérience, de collecter des objets et d'améliorer leur personnage. Elle est construite avec Spring Boot et utilise une base de données H2 en mémoire.

## Fonctionnalités

*   **Combat :** Attaquez des monstres manuellement ou automatiquement.
*   **Progression :** Gagnez de l'XP, montez de niveau et améliorez vos statistiques.
*   **Inventaire :** Collectez et utilisez des objets pour vous soigner ou infliger des dégâts.
*   **Logs :** Suivez l'historique des événements du jeu.

## Installation et Démarrage

1.  Clonez ce dépôt.
2.  Assurez-vous d'avoir Java 25 et Maven installés.
3.  Lancez l'application avec la commande :
    ```bash
    mvn spring-boot:run
    ```
4.  L'application sera accessible à l'adresse `http://localhost:8080`.

## Objectifs Pédagogiques

Ce projet contient volontairement des vulnérabilités et des problèmes de conception. Votre mission est d'analyser le code source, d'identifier ces faiblesses et de proposer des améliorations.

Bonne chasse aux bugs !

## SonarQube (Docker)

L'environnement SonarQube local est fourni via `docker-compose.sonarqube.yml`.

1. Demarrer SonarQube:
   ```bash
   docker compose -f docker-compose.sonarqube.yml up -d
   ```
2. Ouvrir l'interface:
   - `http://localhost:9000`
3. Verifier les services:
   ```bash
   docker compose -f docker-compose.sonarqube.yml ps
   ```
4. Arreter l'environnement:
   ```bash
   docker compose -f docker-compose.sonarqube.yml down
   ```
