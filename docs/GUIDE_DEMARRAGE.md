# Guide de Démarrage - Tfakkarni Platform

Ce guide vous explique comment démarrer l'application pour tester les nouvelles interfaces Quiz et Equipment.

## 📋 Prérequis

Assurez-vous d'avoir installé :
- **Java 17+**
- **Maven 3.9+**
- **Node.js 20+**
- **pnpm** (installer avec `npm install -g pnpm`)

## 🚀 Étapes de Démarrage

### Étape 1 : Démarrer Keycloak (Authentification)

Ouvrez un terminal et exécutez :

```bash
cd keycloak/bin
.\kc.bat start-dev --http-port=8280      # Windows
# OU
./kc.sh start-dev --http-port=8280     # macOS / Linux
```

✅ Keycloak sera accessible sur : http://localhost:8280
- Admin Console : http://localhost:8280/admin
- Identifiants par défaut : `admin` / `admin`

---

### Étape 2 : Démarrer Eureka (Service Discovery)

Ouvrez un **nouveau terminal** :

```bash
cd backend/discovery-service
mvn spring-boot:run
```

✅ Eureka sera accessible sur : http://localhost:8761

**Attendez** que vous voyiez `Started EurekaServerApplication` avant de continuer.

---

### Étape 3 : Démarrer les Services Backend

Ouvrez **6 terminaux séparés** (un pour chaque service) :

#### Terminal 1 - User Service
```bash
cd backend/user-service
mvn spring-boot:run
```

#### Terminal 2 - Game Service (contient Quiz)
```bash
cd backend/game-service
mvn spring-boot:run
```

#### Terminal 3 - Medical Service (contient Equipment)
```bash
cd backend/medical-service
mvn spring-boot:run
```

#### Terminal 4 - Tracking Service
```bash
cd backend/tracking-service
mvn spring-boot:run
```

#### Terminal 5 - Alert Service
```bash
cd backend/alert-service
mvn spring-boot:run
```

#### Terminal 6 - ML Service
```bash
cd backend/ml-service
mvn spring-boot:run
```

**Vérifiez** que tous les services sont démarrés en consultant Eureka : http://localhost:8761

---

### Étape 4 : Démarrer l'API Gateway

Ouvrez un **nouveau terminal** :

```bash
cd backend/api-gateway
mvn spring-boot:run
```

✅ API Gateway sera accessible sur : http://localhost:9090

---

### Étape 5 : Démarrer le Frontend

Ouvrez un **nouveau terminal** :

```bash
cd frontend
pnpm install          # Installe les dépendances (première fois seulement)
pnpm start            # Démarre le serveur de développement
```

✅ Le frontend sera accessible sur : http://localhost:4200

Le navigateur s'ouvrira automatiquement. Sinon, ouvrez manuellement http://localhost:4200

---

## 🧪 Tester les Nouvelles Interfaces

### Pour le Helper (Dashboard)

1. **Connectez-vous** avec un compte Helper
2. Dans le **Dashboard**, vous verrez deux nouvelles cartes :
   - 📝 **Quiz Management** - Gérer les quiz
   - 🏥 **Equipment** - Gérer l'équipement médical

3. **Quiz Management** :
   - Cliquez sur "Quiz Management"
   - Créez un nouveau quiz avec un topic
   - Consultez les statistiques (total, score moyen)

4. **Equipment Management** :
   - Cliquez sur "Equipment"
   - Consultez les équipements disponibles
   - Gérez vos prêts actifs

### Pour le Patient (Vue Simplifiée)

1. **Connectez-vous** avec un compte Patient
2. Dans la **vue patient**, vous verrez deux nouveaux boutons :
   - 📝 **Quizzes** - Passer des quiz
   - 🏥 **Equipment** - Voir l'équipement

3. **Quizzes** :
   - Cliquez sur "Quizzes"
   - Sélectionnez un quiz disponible
   - Répondez aux questions
   - Consultez votre score

4. **Equipment** :
   - Cliquez sur "Equipment"
   - Consultez les équipements disponibles
   - Voir vos prêts actifs

---

## 🔍 Vérification des Services

### Vérifier que tout fonctionne :

1. **Eureka Dashboard** : http://localhost:8761
   - Tous les services doivent être enregistrés (6 services + gateway)

2. **API Gateway** : http://localhost:9090
   - Devrait répondre (peut nécessiter une authentification)

3. **Frontend** : http://localhost:4200
   - La page d'accueil devrait s'afficher

---

## 🐛 Dépannage

### Problème : Les services ne démarrent pas

- Vérifiez que Java 17+ est installé : `java -version`
- Vérifiez que Maven est installé : `mvn -version`
- Vérifiez que les ports ne sont pas déjà utilisés

### Problème : Le frontend ne se connecte pas au backend

- Vérifiez que l'API Gateway est démarré (port 9090)
- Vérifiez que tous les services backend sont démarrés
- Vérifiez la console du navigateur pour les erreurs CORS

### Problème : Erreurs 404 sur les endpoints Quiz/Equipment

- Vérifiez que `game-service` et `medical-service` sont démarrés
- Vérifiez les logs des services pour les erreurs
- Vérifiez que les routes dans `application.yml` du gateway sont correctes
- **Important** : Si vous obtenez des erreurs 404, vérifiez que les contrôleurs backend utilisent les bons chemins :
  - Les contrôleurs Quiz utilisent `/api/quiz` mais le gateway route `/api/games/**`
  - Vous devrez peut-être ajuster les URLs dans les services frontend ou modifier les contrôleurs backend

### Problème : Erreurs de compilation TypeScript

```bash
cd frontend
pnpm install  # Réinstaller les dépendances
```

---

## 📝 Notes Importantes

1. **Ordre de démarrage** :
   - Keycloak → Eureka → Services Backend → API Gateway → Frontend

2. **Ports utilisés** :
   - Keycloak : 8280
   - Eureka : 8761
   - API Gateway : 9090
   - Frontend : 4200
   - User Service : 18081
   - Game Service : 18082
   - Medical Service : 18086
   - Tracking Service : 18083
   - Alert Service : 18084
   - ML Service : 18085

3. **Première utilisation** :
   - Vous devrez créer un compte utilisateur
   - Les données de test peuvent être nécessaires pour voir les quiz/équipements

---

## 🎯 Commandes Rapides (Windows PowerShell)

Si vous utilisez PowerShell, vous pouvez créer un script de démarrage :

```powershell
# Script de démarrage rapide (à adapter selon vos besoins)
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd keycloak/bin; .\kc.bat start-dev --http-port=8280"
Start-Sleep -Seconds 5
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd backend/discovery-service; mvn spring-boot:run"
Start-Sleep -Seconds 10
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd backend/game-service; mvn spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd backend/medical-service; mvn spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd backend/api-gateway; mvn spring-boot:run"
Start-Sleep -Seconds 5
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd frontend; pnpm start"
```

---

Bon test ! 🚀
