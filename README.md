# Tema 2 APD - Planificare task-uri intr-un datacenter
Ioan Teodorescu - 333CB

## Descrierea problemei
Tema consta in implementarea unui sistem de planificare a task-urilor dintr-un datacenter. Acesta le primeste la intervale
regulate de timp si trebuie sa le trimita la mai multe noduri, care acestea la randul lor executa task-urile primite. 
Datacenter-ul are la baza 4 politici de planificare:

1. `Round Robin` - task-urile sunt trimise la nodul (i + 1) % nr_noduri, unde i este numarul ultimului nod care a primit
un task. Pentru ca primul task trebuie sa fie obligatoriu trimit la nodul 0, in `MyDispatcher.java` am folosit variabila 
i, pe care o initializez cu **nr_noduri - 1**. Atunci cand primesc primul task, i-ul devine N si atunci cand calculez
i % N, acesta va fi 0. 

!!! Pentru sincronizare, am folosit semaforul `semaphore` pe post de mutex. Acesta se face acquire cand soseste un thread
cu un task si nimeni nu mai are acces pana cand task-ul este pus in coada nodului destinat prelucrarii acesteia. Scopul
folosirii semaforului este ca nu cumva un task sa ajunga la un nod care nu ii corespunde. Un exemplu ar fi pentru Round Robin,
sa vina doua taskuri la acelasi moment de timp, iar al doilea task sa ajunga la nodul `(i + 1) % n` al primului task si acesta
la nodul celalalt (fiecare nod are timpul lui ramas de executie, iar acest fenomen ar putea aduce la un output incorect)

2. `Shortest Queue` - task-urile sunt distribuite nodurilor in functie de numarul de task-uri pe care acestea le au in
coada sau in executare. Imi atribui un vector `AtomicIntegerArray` in care retin dimensiunea cozii fiecarui nod.
Apoi calculez minimul din acest vector si trimit task-ul la nodul respectiv. Daca exista mai multe noduri cu aceeasi
dimensiune a cozii, aleg nod-ul care al cel mai mic ID.

3. `Size Interval Task Assignment` - task-urile sunt impartite in functie de tipul lor (small, medium, large). Aici vom
avea doar 3 noduri. Nodul 0 primeste task-uri short, al 2-lea, cele medium si ultimul nod, cele large. Pentru a face
asta, iau tipul task-ului (care se afla in clasa `Task`) si un functie de acesta, calculez nodul la care trebuie sa
trimit task-ul.

4. `Least Work Left` - task-urile sunt trimise la nodul care are cel mai putin timp ramas de executare ale celorlalte
task-uri din coada. Pentru a face asta, pentru fiecare nod, aflu timpul ramas de executare al tuturor nodurilor (host.getWorkLeft()).
Apoi, calculez diferenta dintre minimul deja gasit si timpul ramas de executare al nodului curent. Daca diferenta este
diferenta este mai mica decat 0, atunci nodul curent este cel mai bun candidat pentru a primi task-ul. Daca diferenta
este mai mica decat 30 (asa am stabilit, pentru ca in host fac sleep de 30 de milisecunde intr-un while pentru a executa
taskul) avem caz de egalitate si aici alegem nodul care ID-ul cel mai mic. Actualizez si minimul cu valoarea gasita.

!! Aici se foloseste semaforul `semRunning` din clasa `MyHost` pentru a obtine un timp ramas de executare cat mai apropia
de cel real. Acesta face acquire() pe semafor si se calcula nodul care trebuie sa primeasca task-ul. Dupa ce se face release()

## Implementare Host / Nod
Dupa ce dispatcher-ul primeste task-urile si le distribuie in punctie de politica de planificare, acestea sunt trimise la
noduri care urmeaza sa le execute. Folosesc o coada de prioritate blocanta pentru a retine task-urile. Aceasta are o clasa
TaskPriorityComparator care realizeaza sortarea taskurilor in functie de prioritate. In caz de egalitate se compara timpul
in care task-urile au ajuns in datacenter (start).

In `addTask()`acestea -
sunt adaugate si se face `release()` pe semaforul `semaphone` din main pentru a putea fi adaugate si alte task-uri. In `run()`
se executa task-urile. Thread-uri va lucra intr-un while(), iar atunci cand se va face **shutdown**(), acesta va iesi din while
(_hostRunning_ e `true` cat timp ruleaza, la shutdown se face `false`)

Se ia un task din coada si daca acesta nu e null, il executa. Variabila `taskRunning` este `true` cat timp se executa un task.
(ne va ajuta mai tarziu). Se face acquire pe `semRunning` iar in acesta se verifica daca urmatorul task exista in coada, si daca
acesta are o prioritate mai mare decat pe cel pe care l-am luat sa-l executam si daca task-ul curent este preemptibil. Daca
da, se opreste executatea task-ului curent si se adauga inapoi in coada. Daca nu, se executa task-ul curent. La final, se
face `release()` pe `semRunning` si thread-ul face sleep de 30 de milisecunde (valoare aleasa de mine) pentru a simula
executia task-ului. Dupa ce se temrina sleep-ul, se vor elimina si 30 de milisecunde din timpul ramas de executare al task-ului 
(left). Daca task-ul curent nu mai are timp ramas, asta inseamna ca s-a terminat executarea task-ului, se reseteaza variabilele 
cu care am lucrat si acum se asteapta rularea celolarte taskuri. 

### getQueueSize()
Este folosita in `Shortest Queue`. Returneaza dimensiunea cozii de task-uri. Daca mai avem un task in executare, se adauga
1 la dimensiunea cozii.

### getWorkLeft()
Este folosita in `Least Work Left`. Returneaza timpul ramas de executare al tuturor task-urilor din coada. Se parcurge fiecare
task din coada si se aduna timpul ramas de executare al acestuia. Daca mai avem un task in executare, se adauga si timpul
sau ramas de executare.

