# Race condition — dokaz prije popravke

`mvnw test -Dtest=TicketConcurrencyTest`, prije uvodjenja lockinga.

```
=== IZDAVANJE BROJA ===
pokusaja      : 20
uspjelo       : 6
razlicitih    : 6
gresaka       : 14 [DataIntegrityViolationException]
brojevi       : [T001, T002, T003, T004, T005, T006]

=== POZIVANJE SLJEDECEG ===
saltera       : 20
pozvano       : 20
razlicitih    : 6

Tests run: 2, Failures: 2
```

## Sta ovo znaci

**Izdavanje broja.** Od 20 musterija, samo 6 je dobilo broj. Ostalih 14 je
dobilo gresku. Uzrok: `lastNumber + 1` je citanje pa upis; niti procitaju istu
vrijednost i sve pokusaju upisati isti `sequence_no`. Constraint
`uq_ticket_queue_date_seq` odbije duplikate - baza je sprijecila pogresne
podatke, ali po cijenu 14 neuspjelih zahtjeva.

**Pozivanje sljedeceg.** 20 saltera, 20 "uspjesnih" poziva, ali samo 6
razlicitih ljudi. Znaci 14 puta je pozvan neko ko je vec bio pozvan. Ovdje
nema constrainta koji bi to zaustavio - klasican lost update. Prakticno:
jedan covjek pozvan na vise saltera, ostali preskoceni i nikad na redu.

---

# Poslije popravke

Uvedeno pesimisticko zakljucavanje reda (`SELECT ... FOR UPDATE`).
**Test nije mijenjan** - isti kod, isti brojevi niti.

```
=== IZDAVANJE BROJA ===
pokusaja      : 20
uspjelo       : 20
razlicitih    : 20
gresaka       : 0
brojevi       : [T001 ... T020]

=== POZIVANJE SLJEDECEG ===
saltera       : 20
pozvano       : 20
razlicitih    : 20
gresaka       : 0

Tests run: 2, Failures: 0
```

## Sta je promijenjeno

- `QueueRepository.findWithLockById` sa `@Lock(PESSIMISTIC_WRITE)`
- `QueueService.getEntityForUpdate` sa `@Transactional(propagation = MANDATORY)`
- `TicketService.create` i `callNext` zakljucavaju RED prije citanja

Zakljucava se red, ne ticket: time se operacije nad istim redom poredaju
u niz, a isti redoslijed zakljucavanja u obje metode iskljucuje deadlock.

## Zasto pesimisticki, a ne optimisticki

Brojac `lastNumber` je tacka nadmetanja po prirodi - svaka musterija u
istom redu ide na istu vrijednost. Optimisticki pristup (`@Version`) je
bolji kad su sukobi rijetki: pusti da se desi, otkrij ga pri upisu, ponovi.
Ovdje bi sukob bio pravilo a ne izuzetak, pa bi se posao stalno ponavljao.

Pesimisticki lock kosta cekanje, ali samo za isti red - dvije razlicite
lokacije se ne blokiraju medjusobno.
