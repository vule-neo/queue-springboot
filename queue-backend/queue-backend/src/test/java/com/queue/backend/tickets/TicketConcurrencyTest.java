package com.queue.backend.tickets;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.queue.backend.locations.Location;
import com.queue.backend.locations.LocationRepository;
import com.queue.backend.organizations.Organization;
import com.queue.backend.organizations.OrganizationRepository;
import com.queue.backend.queues.Queue;
import com.queue.backend.queues.QueueRepository;
import com.queue.backend.servicetypes.ServiceType;
import com.queue.backend.servicetypes.ServiceTypeRepository;

/**
 * Dokazuje race condition kod izdavanja brojeva i pozivanja sljedeceg.
 *
 * NAMJERNO NEMA @Transactional NA KLASI. @Transactional na testu drzi sve
 * u JEDNOJ transakciji, pa se niti ne bi ni nadmetale - trka se ne bi
 * mogla reprodukovati. Zato i ciscenje radimo rucno u @AfterEach.
 */
@SpringBootTest
@DisplayName("Concurrency: izdavanje broja i pozivanje sljedeceg")
class TicketConcurrencyTest {

    private static final int BROJ_NITI = 20;

    @Autowired private TicketService ticketService;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private QueueRepository queueRepository;
    @Autowired private ServiceTypeRepository serviceTypeRepository;
    @Autowired private LocationRepository locationRepository;
    @Autowired private OrganizationRepository organizationRepository;

    private Queue red;

    @BeforeEach
    void pripremi() {
        String biljeg = "test-" + System.nanoTime();

        Organization org = new Organization();
        org.setName("Test Org");
        org.setSlug(biljeg);
        org = organizationRepository.save(org);

        Location lok = new Location();
        lok.setOrganization(org);
        lok.setName("Test Lokacija");
        lok = locationRepository.save(lok);

        ServiceType usluga = new ServiceType();
        usluga.setOrganization(org);
        usluga.setName("Test Usluga");
        usluga.setAvgDurationMinutes(5);
        usluga = serviceTypeRepository.save(usluga);

        Queue q = new Queue();
        q.setLocation(lok);
        q.setServiceType(usluga);
        q.setPrefix("T");
        red = queueRepository.save(q);
    }

    @AfterEach
    void ocisti() {
        ticketRepository.deleteAll(
                ticketRepository.findByQueueIdAndIssuedDateOrderBySequenceNoAsc(
                        red.getId(), java.time.LocalDate.now()));
        Long lokId = red.getLocation().getId();
        Long uslugaId = red.getServiceType().getId();
        Long orgId = red.getLocation().getOrganization().getId();
        queueRepository.deleteById(red.getId());
        serviceTypeRepository.deleteById(uslugaId);
        locationRepository.deleteById(lokId);
        organizationRepository.deleteById(orgId);
    }

    @Test
    @DisplayName("20 istovremenih zahtjeva mora dati 20 razlicitih brojeva")
    void izdavanjeBroja() throws Exception {
        ConcurrentLinkedQueue<String> brojevi = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<String> greske = new ConcurrentLinkedQueue<>();

        pokreniParalelno(() -> {
            try {
                brojevi.add(ticketService.create(red.getId()).number());
            } catch (Exception e) {
                greske.add(e.getClass().getSimpleName());
            }
        });

        long razliciti = brojevi.stream().distinct().count();

        System.out.println("\n=== IZDAVANJE BROJA ===");
        System.out.println("pokusaja      : " + BROJ_NITI);
        System.out.println("uspjelo       : " + brojevi.size());
        System.out.println("razlicitih    : " + razliciti);
        System.out.println("gresaka       : " + greske.size() + " " + greske.stream().distinct().toList());
        System.out.println("brojevi       : " + brojevi.stream().sorted().toList());

        assertThat(greske).as("nijedan zahtjev ne smije puci").isEmpty();
        assertThat(brojevi).as("svi moraju dobiti broj").hasSize(BROJ_NITI);
        assertThat(razliciti).as("svi brojevi moraju biti razliciti").isEqualTo(BROJ_NITI);
    }

    @Test
    @DisplayName("5 saltera ne smije pozvati istog covjeka")
    void pozivanjeSljedeceg() throws Exception {
        // Prvo mirno izdamo 20 brojeva, jedan po jedan - ovdje trka ne smeta.
        for (int i = 0; i < BROJ_NITI; i++) {
            ticketService.create(red.getId());
        }

        ConcurrentLinkedQueue<Long> pozvaniIdevi = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<String> greske = new ConcurrentLinkedQueue<>();

        pokreniParalelno(() -> {
            try {
                pozvaniIdevi.add(ticketService.callNext(red.getId()).id());
            } catch (Exception e) {
                greske.add(e.getClass().getSimpleName());
            }
        });

        long razliciti = pozvaniIdevi.stream().distinct().count();

        System.out.println("\n=== POZIVANJE SLJEDECEG ===");
        System.out.println("saltera       : " + BROJ_NITI);
        System.out.println("pozvano       : " + pozvaniIdevi.size());
        System.out.println("razlicitih    : " + razliciti);
        System.out.println("gresaka       : " + greske.size() + " " + greske.stream().distinct().toList());

        assertThat(razliciti)
                .as("isti covjek ne smije biti pozvan dvaput")
                .isEqualTo(pozvaniIdevi.size());
    }

    /**
     * Pusti BROJ_NITI niti da urade posao u ISTOM trenutku.
     *
     * Bez startne kapije bi se niti razvukle: prva bi zavrsila prije nego
     * zadnja i krene, pa se trka ne bi ni desila.
     */
    private void pokreniParalelno(Runnable posao) throws InterruptedException {
        ExecutorService bazen = Executors.newFixedThreadPool(BROJ_NITI);
        CountDownLatch startnaKapija = new CountDownLatch(1);
        CountDownLatch cilj = new CountDownLatch(BROJ_NITI);

        for (int i = 0; i < BROJ_NITI; i++) {
            bazen.submit(() -> {
                try {
                    startnaKapija.await();   // cekaj sve ostale
                    posao.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    cilj.countDown();        // javi da si gotov
                }
            });
        }

        startnaKapija.countDown();           // START - svi krecu odjednom
        boolean svrsili = cilj.await(60, TimeUnit.SECONDS);
        bazen.shutdown();

        assertThat(svrsili).as("niti nisu zavrsile na vrijeme").isTrue();
    }

    @SuppressWarnings("unused")
    private static List<String> neiskorisceno() { return List.of(); }
}
