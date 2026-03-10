---
title: "Connect"
description: "Associez votre téléphone au compagnon de bureau Spyglass Connect pour consulter votre inventaire, trouver des coffres, localiser des structures et explorer une carte — le tout via le WiFi local."
subtitle: "Votre monde Minecraft, sur votre téléphone."
cssClass: "connect-page"
---

> **Logiciel alpha** — Spyglass Connect est en cours de développement actif. Des bugs, des fonctionnalités manquantes et des imperfections sont à prévoir.

## Aperçu

Spyglass Connect est une application compagnon de bureau qui lit les fichiers de sauvegarde de Minecraft Java Edition et diffuse les données vers votre téléphone via le WiFi local. Pas de serveurs cloud, pas de comptes — tout reste sur votre réseau.

**[Télécharger Spyglass Connect pour le bureau](https://github.com/beryndil/Spyglass-Connect)**

## Comment ça marche

1. **Lancez** Spyglass Connect sur votre PC (Windows, macOS ou Linux)
2. **Scannez** le QR code affiché sur votre PC depuis l'application Spyglass sur votre téléphone
3. **C'est fait** — votre téléphone se reconnecte automatiquement dès que les deux appareils sont sur le même WiFi

En coulisses : le QR code appaire les appareils via un échange de clés ECDH, puis communique via un WebSocket chiffré (AES-256-GCM). Le mDNS gère la reconnexion automatique.
La négociation de version du protocole garantit la compatibilité des deux applications — si l'une des deux est obsolète, un message d'erreur clair s'affichera.

## Fonctionnalités

### Visionneuse de personnage

Visualisez l'équipement complet de votre joueur — armure, objets en main, main secondaire et toutes les statistiques. Appuyez sur n'importe quel objet pour accéder à sa page de détail complète dans Spyglass.

### Visionneuse d'inventaire

Parcourez votre inventaire complet, les emplacements d'armure, la main secondaire et le contenu du coffre de l'Ender. Chaque objet est lié à la base de données Spyglass.

### Chercheur de coffres

Recherchez n'importe quel objet dans **tous les conteneurs** de votre monde — coffres, tonneaux, boîtes de shulker, entonnoirs et plus encore. Les résultats affichent le type de conteneur, les coordonnées et le nombre d'objets.

### Localisateur de structures

Trouvez des villages, temples, monuments, forteresses et toutes les autres structures générées. Les résultats incluent les coordonnées et la distance depuis votre position actuelle.

### Carte aérienne

Une carte interactive du terrain affichant les marqueurs de structures, la position de votre joueur et les caractéristiques du terrain. Zoomez et faites glisser pour explorer votre monde.

## Configuration requise

- [Spyglass Connect](https://github.com/beryndil/Spyglass-Connect) en cours d'exécution sur votre PC
- Les deux appareils sur le même réseau WiFi
- Fichiers de sauvegarde Minecraft Java Edition accessibles sur votre PC
- Spyglass Connect protocole v2+ (les deux applications doivent supporter la même version du protocole)
