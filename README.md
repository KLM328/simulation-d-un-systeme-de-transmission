# SIT213 - Simulation d'un système de transmission


## Structure du projet

- **src/** : Contient le code source du projet.
- **bin/** : Contient les fichiers compilés et exécutables.
- **docs/** : Contient la javadoc du projet.
- **tests/** : Contient tous les scripts de test.

## Scripts disponibles à la racine du projet

- **compile** : Compile le code source et génère les fichiers exécutables dans `bin/`.
- **genDoc** : Génère la javadoc à partir des sources et la place dans le dossier `docs/`.
- **cleanAll** : Supprime tous les fichiers générés (compilés et documentation).
- **simulateur** : Lance une simulation conformément aux exigences décrites dans le document "commande unique".

    **Arguments possibles :**

    - `-mess m` :  
    Spécifie le message à émettre ou sa longueur :  
    - Suite de 0 et 1 (au moins 7 bits) = message à transmettre.  
    - Entier ≤ 6 chiffres = longueur du message aléatoire à générer.  
    **Par défaut** : message aléatoire de longueur 100.

    - `-seed v` :  
    Initialise les générateurs aléatoires. 
    **Par défaut** : aucune semence (génération aléatoire).

    - `-s `Utilisation de sondes :  
    Active l’utilisation des sondes dans la  simulation.  
    **Par défaut** : non utilisé

    - `-form f`
    Utilisation d'une transmission analogique, `f` précise la forme d'onde
    - NRZ forme d'onde rectangulaire
    - NRZT forme d'onde trapézoïdale
    - RZ forme d'onde impulsionnelle 
     **Par défaut** : forme d'onde RZ

     - `-nbEch ne` 
     Utilisation d'une transmission analogique, `ne` indique le nombre d'échantillons par bit
     **Par défaut** : 30 échantillons par bit.

     - `-ampl min max`
     `min` et `max` précisent l'amplitude min et max du signal en valeurs flotantes. Attention min<max.
    **Par défaut** : 0.0f comme min et 1.0f comme max.

    - `-snrpb s`
    `s` précise la valeur du rapport signal sur bruit par bit en dB (valeurs flottantes).n
    **Par défaut** : la transmission est non bruitée.

     - `-ti dt ar `
    Utilisation d’une transmission analogique multi-trajets 
    `dt` : décalage (en échantillons) entre trajets direct et indirect ; `ar`: amplitude relative.
    `dt` entier, `ar` flottant.
    Jusqu’à 5 couples peuvent suivre -ti pour simuler plusieurs trajets indirects.
    **Par défaut** : pas de trajets indirects (0 et 0.0f pour des trajets directs)

     - `-codeur `
    Active l'utilisation d'un codeur et d'un décodeur pour faire du codage de canal
    **Par défaut** : le simulateur n'utilise pas de codage de canal.


- **runTests** : Exécute automatiquement tous les scripts présents dans le dossier `tests/`.

## Utilisation

### Compilation du projet
Pour compiler le projet, exécuter le script `compile` depuis la racine du projet :
**./compile.sh**
Puis utiliser le script `simulateur` avec les options souhaitées.
exemple : **./simulateur.sh -s -message 0111000101**
