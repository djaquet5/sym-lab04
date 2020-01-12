# SYM - Laboratoire 04

## Capteurs et Bluetooth Low Energy

Auteurs : Lionel Burgbacher, David Jaquet, Jeremy Zerbib

## Introduction

Ce laboratoire est la seconde partie du travail concernant l'utilisation de données environnementales, celui-ci est consacré aux capteurs disponibles sur les smartphones (accéléromètre et magnétomètre principalement) ainsi qu'à la communication *Bluetooth Low Energy*.

## Questions

### Capteurs

**Une fois la manipulation effectuée, vous constaterez que les animations de la flèche ne sont pas fluides, il va y avoir un tremblement plus ou moins important même si le téléphone ne bouge pas. Veuillez expliquer quelle est la cause la plus probable de ce tremblement et donner une manière (sans forcément l’implémenter) d’y remédier.**

En effet, nous avons pu constater que les animations ne sont pas fluides. Nous avons fait le test en bougeant l'écran et sans le bouger. La flèche était en constante oscillation.

Ceci est sûrement dû à notre implémentation "simpliste" et naïve qui veut que nous ayons simplement à autoriser les capteurs à être utilisés (`registerListener()`). Le soucis de cette approche est que nous pouvons observer des valeurs sur les axes de la matrice de rotation qui sont initialisées à 0. 

Une des corrections trouvées pour avoir une "meilleure" précision est de secouer l'appareil et les capteurs s'initialisent correctement. Cette solution n'est pas une solution viable car elle n'est pas fonctionnelle dans certains cas.

Une autre solution est de modifier la méthode `onAccuracyChanged()` et de lui faire retourner les bonnes valeurs sur l'état du capteur. Dans notre cas, nous voyons que la précision est toujours en *haute* quand elle devrait être en *imprécise* (respectivement ` SensorManager.SENSOR_STATUS_ACCURACY_HIGH` et `SensorManager.SENSOR_STATUS_UNRELIABLE`). Avec cette méthode correctement implémentée, les tremblements devraient cesser.

### Bluetooth Low Energie (BLE)

**La caractéristique permettant de lire la température retourne la valeur en degrés Celsius, multipliée par 10, sous la forme d’un entier non-signé de 16 bits. Quel est l’intérêt de procéder de la sorte ? Pourquoi ne pas échanger un nombre à virgule flottante de type float par exemple ?**

Le type `float` est encodé sur 32 bits et a une précision faible pour les nombres décimaux comparé, par exemple, au type `double`.

De plus, puisque nous parlons de la température d'un appareil électronique, nous n'avons pas besoin d'une grande plage de température. A titre de comparaison, un cpu de type `i7` a une température conseillée de `100°C`. Le problème de précision du `float` est donc compensé grâce à ce changement de type.

**Le niveau de charge de la pile est à présent indiqué uniquement sur l’écran du périphérique, mais nous souhaiterions que celui-ci puisse informer le smartphone sur son niveau de charge restante. Veuillez spécifier la(les) caractéristique(s) qui composerai(en)t un tel service, mis à disposition par le périphérique et permettant de communiquer le niveau de batterie restant via Bluetooth Low Energy. Pour chaque caractéristique, vous indiquerez les opérations supportées (lecture, écriture, notification, indication, etc.) ainsi que les données échangées et leur format.**

D'après la documentation officielle [Bluetooth](https://www.bluetooth.com/specifications/gatt/services/), le service permettant de d'informer un smartphone est le service `Battery Service` répondant au caractéristiques suivantes :

| Name            | Uniform Type Identifier               | Assigned Number | Specification |
| --------------- | ------------------------------------- | --------------- | ------------- |
| Battery Service | org.bluetooth.service.battery_service | 0x180F          | GSS           |

Cette caractéristique renvoie le niveau actuel de batterie en pourcentage compris entre 0 et 100. Un pourcentage nul (0%) représente une batterie complétement déchargée tandis qu'un pourcentage égal à 100 représente une batterie chargée.

Les données sont au format `UInt8`. Nous avons une plage de valeur correspondant à l'intervalle `[0, 255]`. Les valeurs comprises entre 101 et 255 y compris ne sont donc pas utilisées.

En suivant le lien fourni plus haut, nous pouvons obtenir un fichier [xml](https://www.bluetooth.com/wp-content/uploads/Sitecore-Media-Library/Gatt/Xml/Services/org.bluetooth.service.battery_service.xml) qui nous permet d'obtenir les opérations suivantes :

| Opération             | Authorisation |
| --------------------- | ------------- |
| Lecture               | Obligatoire   |
| Ecriture              | Exclu         |
| Ecriture sans réponse | Exclu         |
| Ecriture signée       | Exclu         |
| Ecriture fiable       | Exclu         |
| Notification          | Optionnel     |
| Indication            | Exclu         |
| Ecriture auxiliaire   | Exclu         |
| Diffusion             | Exclu         |

