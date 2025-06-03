# TODO List - Import HRMS vers ERPNext

## 1. Fonction Upload et Validation des Fichiers

### 1.1 Interface Upload
- [ ] Créer endpoint POST `/api/import/upload`
- [ ] Implémenter drag & drop avec 3 champs séparés :
  - [ ] Fichier Employés (fichier1.csv)
  - [ ] Fichier Structure Salariale (fichier2.csv) 
  - [ ] Fichier Attribution Salaires (fichier3.csv)
- [ ] Validation format fichier (.csv uniquement)
- [ ] Validation taille fichier (max 10MB)
- [ ] Stockage temporaire des fichiers uploadés

### 1.2 Parsing et Prévisualisation
- [ ] Parser CSV avec gestion encodage UTF-8/ISO-8859-1
- [ ] Détecter automatiquement le délimiteur (virgule/point-virgule)
- [ ] **Vérifier présence des colonnes obligatoires AVANT traitement**
- [ ] Afficher 5 premières lignes pour vérification
- [ ] Mapper automatiquement les colonnes vers les champs ERPNext
- [ ] Gérer les en-têtes avec/sans accents
- [ ] **Proposer solutions pour colonnes manquantes critiques**

## 2. Fonction Validation des Données

### 2.1 Validation Fichier Employés (Employee)
- [ ] Vérifier format Ref (numérique obligatoire)
- [ ] Valider Nom/Prénom (non vides, max 140 caractères)
- [ ] Contrôler genre ("Masculin"/"Feminin" → "Male"/"Female")
- [ ] Valider dates (DD/MM/YYYY → YYYY-MM-DD)
  - [ ] Date embauche cohérente (pas future)
  - [ ] Date naissance cohérente (âge entre 16-70 ans)
- [ ] Vérifier unicité de la Ref employé
- [ ] **Vérifier/Créer Company automatiquement si inexistante**

### 2.2 Validation Structure Salariale
- [ ] Vérifier salary_structure unique par company
- [ ] Valider name/abbr (non vides, pas de doublons)
- [ ] Contrôler type ("earning"/"deduction")
- [ ] Valider formules dans valeur :
  - [ ] Parser expressions mathématiques (SB * 0.3)
  - [ ] Vérifier références vers autres composants
- [ ] **Vérifier/Créer Company automatiquement si inexistante**

### 2.3 Validation Attribution Salaires
- [ ] Valider format date (DD/MM/YYYY)
- [ ] Vérifier Ref Employe existe dans fichier1 ou ERPNext
- [ ] Contrôler Salaire Base (numérique > 0)
- [ ] Valider salary_structure existe dans fichier2 ou ERPNext
- [ ] Détecter doublons (même employé, même mois, même structure)
- [ ] **Vérifier/Créer données de référence manquantes automatiquement**

## 2.5 Fonction Auto-Création des Données de Référence

### 2.5.1 Vérification/Création Company
- [ ] GET `/api/resource/Company/{company_name}` pour vérifier existence
- [ ] Si inexistante, créer automatiquement :
  ```json
  {
    "doctype": "Company",
    "company_name": "My Company",
    "abbr": "MC",
    "default_currency": "MGA",
    "country": "Madagascar"
  }
  ```
- [ ] Logger création automatique pour traçabilité

### 2.5.2 Vérification/Création Employee (mise à jour vs création)
- [ ] GET `/api/resource/Employee?filters=[["employee_number","=","{ref}"]]`
- [ ] Si existe : 
  - [ ] Comparer données actuelles vs import
  - [ ] Proposer mise à jour si différences détectées
  - [ ] Demander confirmation utilisateur
- [ ] Si inexistant : créer automatiquement

### 2.5.3 Vérification/Création Salary Components
- [ ] Vérifier existence de chaque component par name+company :
  - [ ] GET `/api/resource/Salary Component/{component_name}`
- [ ] Créer automatiquement si manquant :
  ```json
  {
    "doctype": "Salary Component",
    "salary_component": "Salaire Base",
    "salary_component_abbr": "SB", 
    "type": "Earning",
    "company": "My Company"
  }
  ```

### 2.5.4 Vérification/Création Salary Structure
- [ ] GET `/api/resource/Salary Structure?filters=[["name","=","{structure_name}"]]`
- [ ] Si inexistante, créer avec tous ses components :
  ```json
  {
    "doctype": "Salary Structure",
    "name": "gasy1",
    "company": "My Company",
    "earnings": [
      {"salary_component": "Salaire Base", "formula": "base", "amount": 0},
      {"salary_component": "Indemnité", "formula": "SB * 0.3", "amount": 0}
    ],
    "deductions": [
      {"salary_component": "Taxe sociale", "formula": "(SB + IND) * 0.2", "amount": 0}
    ]
  }
  ```

## 2.6 Fonction Gestion des Colonnes Manquantes Critiques

### 2.6.1 Détection des Colonnes Obligatoires
- [ ] **Fichier1 (Employee) - Colonnes critiques :**
  - [ ] `Ref` (obligatoire - clé de liaison)
  - [ ] `Nom` (obligatoire ERPNext)  
  - [ ] `Prenom` (obligatoire ERPNext)
  - [ ] `company` (obligatoire ERPNext)
- [ ] **Fichier2 (Salary Structure) - Colonnes critiques :**
  - [ ] `salary structure` (obligatoire - clé de liaison)
  - [ ] `name` (obligatoire)
  - [ ] `type` (obligatoire - earning/deduction)
- [ ] **Fichier3 (Assignment) - Colonnes critiques :**
  - [ ] `Ref Employe` (obligatoire - liaison Employee)
  - [ ] `Salaire` (obligatoire - liaison Salary Structure)
  - [ ] `Mois` (obligatoire)

### 2.6.2 Stratégies de Résolution Automatique

#### Stratégie 1: Mapping Intelligent des Noms de Colonnes
- [ ] Créer dictionnaire de correspondances :
  ```java
  Map<String, List<String>> columnMappings = {
    "Ref" → ["ref", "id", "employee_id", "emp_id", "numero", "reference"],
    "Nom" → ["nom", "lastname", "family_name", "surname"],
    "Prenom" → ["prenom", "firstname", "given_name"],
    "Ref Employe" → ["ref_employe", "employee_ref", "emp_ref", "ref"]
  }
  ```
- [ ] Scanner en-têtes avec variations (accents, casse, espaces)
- [ ] Proposer mapping automatique avec score de confiance

#### Stratégie 2: Génération Automatique de Valeurs
- [ ] **Si `Ref` manquante :**
  - [ ] Générer séquence auto-incrémentée (1, 2, 3...)
  - [ ] Utiliser numéro de ligne comme Ref
  - [ ] Proposer pattern personnalisé (EMP001, EMP002...)
- [ ] **Si `company` manquante :**
  - [ ] Utiliser valeur par défaut "My Company"
  - [ ] Demander saisie unique pour tout le fichier
- [ ] **Si `Mois` manquante (fichier3) :**
  - [ ] Utiliser mois courant par défaut
  - [ ] Proposer saisie manuelle du mois d'affectation

#### Stratégie 3: Colonnes Dérivées/Calculées
- [ ] **Si `employee_name` manquante :**
  - [ ] Générer : `Prenom + " " + Nom`
- [ ] **Si `salary structure` manquante (fichier3) :**
  - [ ] Utiliser valeur unique du fichier2 si une seule structure
  - [ ] Proposer mapping par défaut
- [ ] **Si `abbr` manquante (fichier2) :**
  - [ ] Générer depuis `name` (premiers caractères)

### 2.6.3 Interface de Résolution Interactive

#### Écran de Mapping des Colonnes
- [ ] Afficher tableau de correspondances :
  ```
  Colonne Requise    | Colonne Trouvée     | Action
  ------------------|--------------------|---------
  Ref               | ❌ Manquante        | [Générer Auto] [Mapper Colonne]
  Nom               | ✅ Nom              | OK
  Ref Employe       | ❌ Manquante        | [Utiliser Ref] [Mapper Colonne]
  ```
- [ ] Permettre drag&drop pour mapper colonnes
- [ ] Prévisualiser résultat du mapping

#### Options de Résolution
- [ ] **Génération Automatique :**
  - [ ] Checkbox "Générer Ref automatiquement (1,2,3...)"
  - [ ] Pattern personnalisé : `[PREFIX][0000]`
- [ ] **Mapping Manuel :**
  - [ ] Dropdown avec colonnes disponibles
  - [ ] Aperçu des valeurs (5 premières lignes)
- [ ] **Valeur par Défaut :**
  - [ ] Champ texte pour valeur unique
  - [ ] Validation en temps réel

### 2.6.4 Validation Post-Résolution
- [ ] Vérifier unicité des Ref générées
- [ ] Contrôler cohérence des liaisons :
  - [ ] Toutes les `Ref Employe` existent dans Employee
  - [ ] Toutes les `salary structure` existent dans Structure
- [ ] Alerter si résolution introduit des doublons

## 3. Fonction Transformation des Données

### 3.1 Transformation Employés
- [ ] Convertir genre : "Masculin"→"Male", "Feminin"→"Female"
- [ ] Transformer dates : "DD/MM/YYYY" → "YYYY-MM-DD"
- [ ] Générer employee_name : "Prenom Nom"
- [ ] Créer naming_series automatique
- [ ] Mapper vers champs ERPNext Employee :
  ```
  employee_number → Ref
  first_name → Prenom  
  last_name → Nom
  gender → genre (transformé)
  date_of_joining → Date embauche
  date_of_birth → date naissance
  company → company
  ```

### 3.2 Transformation Structure Salariale
- [ ] Créer Salary Structure par salary_structure unique
- [ ] Générer Salary Components pour chaque ligne
- [ ] Transformer formules pour ERPNext :
  - [ ] "base" → montant fixe
  - [ ] "SB * 0.3" → formule conditionnelle
- [ ] Organiser earnings vs deductions

### 3.3 Transformation Attribution Salaires  
- [ ] Convertir dates : "DD/MM/YYYY" → "YYYY-MM-DD"
- [ ] Créer Salary Structure Assignment
- [ ] Lier employee (par Ref) et salary_structure
- [ ] Définir from_date et to_date appropriés

## 4. Fonction Import vers ERPNext

### 4.1 Ordre d'importation avec Auto-Création (respecter dépendances)
1. [ ] **Company** (créer automatiquement si manquante)
2. [ ] **Salary Components** (créer automatiquement depuis fichier2)  
3. [ ] **Employee** (créer/mettre à jour selon existence)
4. [ ] **Salary Structure + Components** (créer si manquante)
5. [ ] **Salary Structure Assignment** (dépend de tous les précédents)

### 4.2 Appels API ERPNext
- [ ] Configurer authentification ERPNext (API Key/Token)
- [ ] Implémenter retry logic (3 tentatives)
- [ ] Gérer timeouts (30s par requête)
- [ ] Logger toutes les requêtes/réponses

#### Auto-Création Company
- [ ] GET `/api/resource/Company/{company_name}` pour vérifier
- [ ] Si 404, POST `/api/resource/Company` avec données par défaut
- [ ] Utiliser nom du fichier comme company_name si pas spécifié

#### Employee Import avec Gestion Existence
- [ ] GET `/api/resource/Employee?filters=[["employee_number","=","{ref}"]]`
- [ ] Si existe : PUT `/api/resource/Employee/{name}` pour mise à jour
- [ ] Si n'existe pas : POST `/api/resource/Employee` pour création
- [ ] Récupérer employee.name généré/existant

#### Auto-Création Salary Structure + Components
- [ ] Pour chaque component du fichier2 :
  - [ ] GET `/api/resource/Salary Component/{name}` 
  - [ ] Si 404 : POST `/api/resource/Salary Component`
- [ ] GET `/api/resource/Salary Structure/{structure_name}`
- [ ] Si 404 : POST `/api/resource/Salary Structure` avec tous components
- [ ] Lier automatiquement components à structure

#### Assignment Import
- [ ] POST `/api/resource/Salary Structure Assignment`
- [ ] Utiliser employee.name récupéré précédemment
- [ ] Gérer périodes de validité

## 5. Fonction Gestion d'Erreurs et Rollback

### 5.1 Stratégie Transactionnelle
- [ ] Sauvegarder état avant import
- [ ] Implémenter rollback en cas d'échec partiel
- [ ] Créer log détaillé des opérations réussies/échouées

### 5.2 Gestion des Erreurs
- [ ] Capturer erreurs de validation
- [ ] Logger erreurs API ERPNext (quota, permissions, etc.)
- [ ] Fournir messages d'erreur explicites à l'utilisateur
- [ ] Permettre correction et re-soumission

## 6. Fonction Reporting et Journalisation

### 6.1 Logging Système
- [ ] Enregistrer qui importe (utilisateur connecté)
- [ ] Timestamp de début/fin d'import
- [ ] Nombre de lignes traitées par fichier
- [ ] Détail des erreurs rencontrées

### 6.2 Rapport Utilisateur
- [ ] Afficher résumé post-import :
  - [ ] X employés créés/mis à jour
  - [ ] Y structures salariales créées  
  - [ ] Z assignments créées
- [ ] Lister erreurs avec numéros de ligne
- [ ] Proposer export du rapport d'import

## 7. Fonction Interface Utilisateur

### 7.1 Workflow UX
- [ ] Page upload avec progress bars
- [ ] Étape validation avec aperçu erreurs
- [ ] Confirmation avant import final
- [ ] Page résultats avec actions possibles

### 7.2 Composants UI
- [ ] Tableau prévisualisation données
- [ ] Indicateurs validation (✓/✗ par fichier)  
- [ ] Modal confirmation d'import
- [ ] Toast notifications succès/erreur

## 8. Tests et Validation

### 8.1 Tests Unitaires
- [ ] Test parsing CSV avec différents formats
- [ ] Test validation données (cas valides/invalides)
- [ ] Test transformation données
- [ ] Mock API ERPNext pour tests

### 8.2 Tests d'Intégration
- [ ] Test import complet sur environnement test ERPNext
- [ ] Test gestion erreurs réseau
- [ ] Test performance (gros volumes)
- [ ] Test concurrent users