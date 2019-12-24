# Laboratoire n°4 : Environnement II

# Capteurs et Bluetooth Low Energy

# Auteurs : Lionel Burgbacher, David Jacquet, Jeremy Zerbib

## Questions

**Une fois la manipulation effectuée, vous constaterez que les animations de la flèche ne sont pas fluides, il va y avoir un tremblement plus ou moins important même si le téléphone ne bouge pas. Veuillez expliquer quelle est la cause la plus probable de ce tremblement et donner une manière (sans forcément l’implémenter) d’y remédier.**

Nous avons pu en effet constater que les animations ne sont pas fluides. Nous avons fait le test en bougeant l'écran et sans le bouger et la flèche était en effet en constante oscillations.

Ceci est sûrement dû à notre implémentation "simpliste" et naïve qui veut que nous ayons simplement à autoriser les capteurs à être utilisés (`registerListener()`). Le soucis de cette approche est que nous pouvons observer des valeurs sur les axes de la matrice de rotation qui sont initialisées à 0. 

Une des corrections trouvées pour avoir une "meilleure" précision est de secouer l'appareil et les capteurs s'initialisent correctement.

Cette solution n'est pas très correcte car elle marche dans la plupart des cas mais pas dans tous les cas.

Une autre solution est de modifier la méthode `onAccuracyChanged()` et de lui faire retourner les bonnes valeurs sur l'état du capteur. Dans notre cas, nous voyons que la précision est toujours en *haute* quand elle devrait être en *imprécise* (respectivement ` SensorManager.SENSOR_STATUS_ACCURACY_HIGH` et `SensorManager.SENSOR_STATUS_UNRELIABLE`). Avec cette méthode correctement implémentée, normalement, les tremblements devraient cesser.

**La caractéristique permettant de lire la température retourne la valeur en degrés Celsius, multipliée par 10, sous la forme d’un entier non-signé de 16 bits. Quel est l’intérêt de procéder de la sorte ? Pourquoi ne pas échanger un nombre à virgule flottante de type float par exemple ?**

**Le niveau de charge de la pile est à présent indiqué uniquement sur l’écran du périphérique, mais nous souhaiterions que celui-ci puisse informer le smartphone sur son niveau de charge restante. Veuillez spécifier la(les) caractéristique(s) qui composerai(en)t un tel service, mis à disposition par le périphérique et permettant de communiquer le niveau de batterie restant via Bluetooth Low Energy. Pour chaque caractéristique, vous indiquerez les opérations supportées (lecture, écriture, notification, indication, etc.) ainsi que les données échangées et leur format.**