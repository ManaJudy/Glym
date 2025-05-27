# Glym

**Glym** est un framework léger, modulaire et moderne écrit en **Kotlin**, inspiré par l’architecture de **Spring**, mais conçu pour être plus simple, plus lisible et plus rapide à prendre en main.

---

## ✨ Présentation

Glym a été pensé pour offrir une alternative minimaliste à Spring, tout en conservant ses concepts fondamentaux : injection de dépendances, architecture en couches, annotations, ORM, et modèle MVC.  
Il permet de créer rapidement des applications web structurées sans la complexité des configurations lourdes.

---

## 🧩 Modules principaux

### `glym-component`
Un conteneur léger pour la gestion des composants (IoC), avec détection automatique via annotations et injection de dépendances simple et fluide.

### `glym-mvc`
Un moteur **Model-View-Controller** basé sur les annotations, qui permet de mapper les routes aux fonctions avec une syntaxe claire et concise.

### `glym-orm`
Un ORM maison, simple mais puissant, pour mapper les entités Kotlin à une base de données relationnelle.  
Il gère également les relations `OneToMany`, `ManyToOne`, etc., via des annotations.

---

## 🚀 Objectifs

- Proposer un **framework léger et pédagogique**, compréhensible de bout en bout
- Offrir une alternative à Spring pour les **petits et moyens projets**
- Mettre en avant les **forces de Kotlin** : concision, null safety, DSLs
- Être **modulaire**, chaque module étant indépendant et réutilisable

---

## 💡 Pourquoi "Glym" ?

Le nom **Glym** évoque une **lueur légère**, une brillance épurée qui reflète la volonté du projet :  
reprendre les concepts puissants d’un grand framework comme Spring, mais avec simplicité et élégance.

---

## 📚 En cours de développement

Glym est encore en phase active de développement. Les premières versions se concentrent sur les fonctionnalités de base suivantes :

- Scan automatique des composants
- Mapping des routes HTTP
- ORM simplifié avec persistance automatique
- Système d’annotation personnalisable

---

## 🛠️ Stack technique

- **Kotlin** 1.9+
- Gestion interne des annotations (pas de dépendance à Spring)
- Compatible avec des serveurs comme Jetty, Ktor, ou Netty

---

## 📦 Installation

_(Section à compléter lorsque le premier artefact sera disponible)_
