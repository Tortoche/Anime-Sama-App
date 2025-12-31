# **Anime-Sama App (Version Fan)**

Bienvenue sur le dépôt de l'application Android non officielle pour **Anime-Sama**. Ce projet a été développé pour offrir une expérience de visionnage optimisée, fluide et persistante sur mobile, en palliant les contraintes habituelles des navigateurs web.

## **🚀 Fonctionnalités Principales**

### **🛡️ Bloqueur de Publicités Intégré**

L'application intègre désormais un système de filtrage avancé pour garantir une navigation sereine :

* **Interception des Popups :** Blocage automatique des nouvelles fenêtres et onglets indésirables souvent déclenchés par les lecteurs vidéo.  
* **Filtrage des URL :** Restriction de la navigation aux domaines légitimes (Anime-Sama et hébergeurs vidéo reconnus) pour éviter les redirections malveillantes.  
* **Nettoyage Visuel :** Suppression des bannières et éléments intrusifs via injection CSS/JS.

### **📍 Redirection Intelligente**

L'application utilise le domaine "boussole" (anime-sama.pw) pour détecter et rediriger automatiquement l'utilisateur vers l'adresse fonctionnelle du site. Cela assure une continuité de service même en cas de changement de nom de domaine.

### **💾 Persistance des Données (Cross-Domain)**

Contrairement à un navigateur classique, cette application sauvegarde votre progression (historique, épisodes vus) localement dans le stockage sécurisé du téléphone.

* **Synchronisation Auto :** Si le site change d'adresse (ex: passage de .tv à .fr), vos données sont automatiquement réinjectées.  
* **Export/Import PC :** Vous pouvez exporter vos données au format JSON compatible avec la version PC pour une transition fluide entre vos appareils.

### **🎨 Interface "Immersive"**

* **Mode Cinéma :** Masquage automatique des barres système (statut, navigation) pour un visionnage plein écran.  
* **Design Unifié :** Les menus et interfaces de l'application reprennent l'identité visuelle du site (thème sombre, accents cyan/bleu nuit) pour une expérience cohérente.

## **📲 Installation**

Cette application n'est pas disponible sur le Google Play Store. Pour l'installer :

1. Rendez-vous dans la section [**Releases**](https://www.google.com/search?q=https://github.com/Tortoche/Anime-Sama-App/releases) de ce dépôt.  
2. Téléchargez le fichier .apk de la dernière version.  
3. Ouvrez le fichier sur votre appareil Android.  
4. Autorisez l'installation d'applications provenant de "Sources inconnues" si demandé.

## **🛠️ Informations Techniques**

Le projet est développé en **Java** sous Android Studio. Il repose sur une WebView hautement configurée :

* **Moteur :** WebView Android avec WebChromeClient personnalisé.  
* **Injection JS :** Utilisation de evaluateJavascript pour la gestion du localStorage et le blocage d'éléments DOM.  
* **Gestion Vidéo :** Implémentation de onShowCustomView pour le support natif du plein écran des lecteurs HTML5.

### **Compilation**

Pour cloner et compiler le projet vous-même :  
git clone \[https://github.com/Tortoche/Anime-Sama-App.git\](https://github.com/Tortoche/Anime-Sama-App.git)

*Prérequis : Android Studio, JDK 17\.*

## **⚠️ Avertissement Légal**

Ce projet est une **initiative personnelle à but non lucratif**. Je ne suis pas affilié à l'équipe d'Anime-Sama.

* L'application agit comme un navigateur spécialisé et n'héberge aucun contenu vidéo.  
* L'utilisation de cette application est sous votre entière responsabilité.

*Développé avec passion pour la communauté.*
