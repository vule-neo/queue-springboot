package com.queue.backend.queues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.queue.backend.common.NotFoundException;
import com.queue.backend.locations.Location;
import com.queue.backend.locations.LocationService;
import com.queue.backend.servicetypes.ServiceType;
import com.queue.backend.servicetypes.ServiceTypeService;

@ExtendWith(MockitoExtension.class)
@DisplayName("QueueService")
class QueueServiceTest {

    @Mock QueueRepository repository;
    @Mock LocationService locationService;
    @Mock ServiceTypeService serviceTypeService;

    @InjectMocks QueueService service;

    @Test
    @DisplayName("create: novi red je zatvoren, brojac na nuli, bez datuma")
    void createDefaults() {
        Location lok = new Location();
        lok.setId(2L);
        ServiceType usluga = new ServiceType();
        usluga.setId(3L);
        when(locationService.getEntity(2L)).thenReturn(lok);
        when(serviceTypeService.getEntity(3L)).thenReturn(usluga);
        when(repository.save(any())).thenAnswer(inv -> {
            Queue q = inv.getArgument(0);
            q.setId(10L);
            return q;
        });

        QueueResponse r = service.create(new CreateQueueRequest(2L, 3L, "B"));

        assertThat(r.id()).isEqualTo(10L);
        assertThat(r.prefix()).isEqualTo("B");
        assertThat(r.open()).isFalse();
        assertThat(r.lastNumber()).isZero();
        assertThat(r.lastNumberDate()).isNull();
    }

    @Test
    @DisplayName("create: nepostojeca lokacija -> NotFound, nista se ne snima")
    void createNepostojecaLokacija() {
        when(locationService.getEntity(2L)).thenThrow(new NotFoundException("Lokacija 2 ne postoji"));

        assertThatThrownBy(() -> service.create(new CreateQueueRequest(2L, 3L, "B")))
                .isInstanceOf(NotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("getEntityForUpdate koristi upit SA lockom, ne obican findById")
    void getEntityForUpdateKoristiLock() {
        Queue q = new Queue();
        q.setId(1L);
        when(repository.findWithLockById(1L)).thenReturn(Optional.of(q));

        assertThat(service.getEntityForUpdate(1L)).isSameAs(q);
        // Da je neko "optimizovao" na findById, lock bi nestao a test bi
        // i dalje prolazio - zato eksplicitno.
        verify(repository, never()).findById(any());
    }

    @Test
    void findByIdNepostojeci() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(99L)).isInstanceOf(NotFoundException.class);
    }
}
