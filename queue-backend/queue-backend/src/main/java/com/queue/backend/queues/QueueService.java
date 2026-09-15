package com.queue.backend.queues;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.queue.backend.common.NotFoundException;
import com.queue.backend.locations.Location;
import com.queue.backend.locations.LocationService;
import com.queue.backend.servicetypes.ServiceType;
import com.queue.backend.servicetypes.ServiceTypeService;

@Service
public class QueueService {

    private final QueueRepository repository;
    private final LocationService locationService;
    private final ServiceTypeService serviceTypeService;

    public QueueService(QueueRepository repository,
                        LocationService locationService,
                        ServiceTypeService serviceTypeService) {
        this.repository = repository;
        this.locationService = locationService;
        this.serviceTypeService = serviceTypeService;
    }

    @Transactional
    public QueueResponse create(CreateQueueRequest request) {
        Location location = locationService.getEntity(request.locationId());
        ServiceType serviceType = serviceTypeService.getEntity(request.serviceId());

        Queue queue = new Queue();
        queue.setLocation(location);
        queue.setServiceType(serviceType);
        queue.setPrefix(request.prefix());
        // open=false, lastNumber=0, lastNumberDate=null ostaju na default vrijednostima.
        // Red se otvara tek u V2, brojac postavlja TicketService pri prvom ticketu.

        return QueueResponse.from(repository.save(queue));
    }

    @Transactional(readOnly = true)
    public List<QueueResponse> findAll() {
        return repository.findAll().stream().map(QueueResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public QueueResponse findById(Long id) {
        return repository.findById(id)
                .map(QueueResponse::from)
                .orElseThrow(() -> new NotFoundException("Red " + id + " ne postoji"));
    }

    /** Sam entitet, bez lockinga - za citanje. */
    @Transactional(readOnly = true)
    public Queue getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Red " + id + " ne postoji"));
    }

    /**
     * Sam entitet, SA zakljucavanjem reda u bazi. Koristi se kad se red
     * mijenja (izdavanje broja) ili kad se na osnovu njega bira ticket.
     *
     * MANDATORY: metoda se SMIJE zvati samo unutar vec otvorene transakcije.
     * Lock vrijedi dok transakcija traje - da ova metoda otvori vlastitu
     * transakciju, lock bi se pustio prije nego pozivalac stigne isto upisati,
     * i ne bi vrijedio nista. Ovako takva greska pukne odmah, umjesto da
     * tiho ne radi.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Queue getEntityForUpdate(Long id) {
        return repository.findWithLockById(id)
                .orElseThrow(() -> new NotFoundException("Red " + id + " ne postoji"));
    }
}
