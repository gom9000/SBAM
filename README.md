# SBAM Modeler – Simple Bus Architecture Machine Modeler

Simulatore a passi fissi della fisica di linee, pin e alimentazioni.


## Il passo

- fase 0: le `Rail` si aggiornano con l'assorbimento del passo precedente
- fase 1: le azioni in scadenza; i pin dichiarano cosa vogliono mettere sulle linee
- fase 2: ogni `Line` calcola la tensione (Millman + RC), i pin leggono con le proprie soglie
