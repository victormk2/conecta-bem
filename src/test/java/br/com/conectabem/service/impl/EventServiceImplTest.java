package br.com.conectabem.service.impl;

import br.com.conectabem.dto.event.EventCreationDTO;
import br.com.conectabem.dto.event.EventListRequest;
import br.com.conectabem.infra.util.Mapper;
import br.com.conectabem.model.Address;
import br.com.conectabem.model.Event;
import br.com.conectabem.model.EventCategory;
import br.com.conectabem.model.User;
import br.com.conectabem.repository.EventRegistrationRepository;
import br.com.conectabem.repository.EventRepository;
import br.com.conectabem.service.AddressService;
import br.com.conectabem.service.CurrentUserService;
import br.com.conectabem.service.UserService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventRegistrationRepository registrationRepository;

    @Mock
    private Mapper<EventCreationDTO, Event> creationToEntity;

    @Mock
    private UserService userService;

    @Mock
    private AddressService addressService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private EventServiceImpl eventService;

    @Nested
    class CreateTest {
        @Test
        void shouldCreateEventSuccessfully() {
            var dto = new EventCreationDTO(
                    "Limpeza do Parque",
                    "Vamos limpar o parque",
                    "00000000-0000-0000-0000-000000000002",
                    "ENVIRONMENT",
                    "2026-04-15T10:00:00",
                    "2026-04-15T14:00:00",
                    50
            );

            var ownerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            var addressId = UUID.fromString("00000000-0000-0000-0000-000000000002");

            var owner = new User();
            owner.setId(ownerId);
            owner.setUsername("john_doe");

            var address = new Address();
            address.setId(addressId);
            address.setCity("São Paulo");

            var eventFromMapper = new Event();
            eventFromMapper.setTitle(dto.title());
            eventFromMapper.setDescription(dto.description());
            eventFromMapper.setCapacity(dto.capacity());
            eventFromMapper.setCategory(EventCategory.ENVIRONMENT);
            eventFromMapper.setStartsAt(LocalDateTime.parse(dto.startsAt()));
            eventFromMapper.setEndsAt(LocalDateTime.parse(dto.endsAt()));

            var savedEvent = new Event();
            savedEvent.setId(UUID.randomUUID());
            savedEvent.setTitle(dto.title());
            savedEvent.setDescription(dto.description());
            savedEvent.setCapacity(dto.capacity());
            savedEvent.setCategory(EventCategory.ENVIRONMENT);
            savedEvent.setStartsAt(LocalDateTime.parse(dto.startsAt()));
            savedEvent.setEndsAt(LocalDateTime.parse(dto.endsAt()));
            savedEvent.setOwner(owner);
            savedEvent.setAddress(address);

            when(creationToEntity.map(dto)).thenReturn(eventFromMapper);
            when(currentUserService.requireUserId()).thenReturn(ownerId);
            when(userService.findById(ownerId)).thenReturn(owner);
            when(addressService.findById(addressId)).thenReturn(address);
            when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

            var result = eventService.create(dto);

            assertThat(result)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("title", "Limpeza do Parque")
                    .hasFieldOrPropertyWithValue("description", "Vamos limpar o parque")
                    .hasFieldOrPropertyWithValue("capacity", 50)
                    .hasFieldOrPropertyWithValue("category", EventCategory.ENVIRONMENT)
                    .hasFieldOrProperty("owner")
                    .hasFieldOrProperty("address");

            assertThat(result.getOwner()).hasFieldOrPropertyWithValue("id", ownerId);
            assertThat(result.getAddress()).hasFieldOrPropertyWithValue("id", addressId);

            verify(creationToEntity).map(dto);
            verify(userService).findById(ownerId);
            verify(addressService).findById(addressId);
            verify(eventRepository).save(any(Event.class));
        }

        @Test
        void shouldIgnoreImageWhenFileIsEmpty() {
            var dto = new EventCreationDTO(
                    "Evento com imagem",
                    "Descricao",
                    "00000000-0000-0000-0000-000000000002",
                    "SOCIAL",
                    "2026-04-15T10:00:00",
                    "2026-04-15T14:00:00",
                    20
            );

            var ownerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            var addressId = UUID.fromString("00000000-0000-0000-0000-000000000002");
            var owner = new User();
            owner.setId(ownerId);

            var address = new Address();
            address.setId(addressId);

            var eventFromMapper = new Event();
            var image = mock(MultipartFile.class);

            when(image.isEmpty()).thenReturn(true);
            when(creationToEntity.map(dto)).thenReturn(eventFromMapper);
            when(currentUserService.requireUserId()).thenReturn(ownerId);
            when(userService.findById(ownerId)).thenReturn(owner);
            when(addressService.findById(addressId)).thenReturn(address);
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

            var result = eventService.createWithImage(dto, image);

            assertThat(result).isSameAs(eventFromMapper);
            assertThat(result.getImage()).isNull();
            verify(eventRepository).save(any(Event.class));
        }

        @Test
        void shouldAttachImageWhenValidImageIsProvided() throws Exception {
            var dto = new EventCreationDTO(
                    "Evento com imagem",
                    "Descricao",
                    "00000000-0000-0000-0000-000000000002",
                    "SOCIAL",
                    "2026-04-15T10:00:00",
                    "2026-04-15T14:00:00",
                    20
            );

            var ownerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            var addressId = UUID.fromString("00000000-0000-0000-0000-000000000002");
            var imageBytes = new byte[]{1, 2, 3};

            var owner = new User();
            owner.setId(ownerId);

            var address = new Address();
            address.setId(addressId);

            var eventFromMapper = new Event();
            var image = mock(MultipartFile.class);

            when(image.isEmpty()).thenReturn(false);
            when(image.getContentType()).thenReturn("image/png");
            when(image.getBytes()).thenReturn(imageBytes);
            when(creationToEntity.map(dto)).thenReturn(eventFromMapper);
            when(currentUserService.requireUserId()).thenReturn(ownerId);
            when(userService.findById(ownerId)).thenReturn(owner);
            when(addressService.findById(addressId)).thenReturn(address);
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

            var result = eventService.createWithImage(dto, image);

            assertThat(result.getImage()).containsExactly(imageBytes);
            verify(eventRepository).save(any(Event.class));
        }
    }

    @Nested
    class ListTest {
        @Test
        void shouldReturnOnlyEventsThatHaveNotEnded() {
            var now = LocalDateTime.now();

            var futureEvent = buildEvent(
                    UUID.randomUUID(), "Limpeza do Parque", EventCategory.ENVIRONMENT,
                    now.plusDays(1), now.plusDays(1).plusHours(4)
            );
            var pastEvent = buildEvent(
                    UUID.randomUUID(), "Evento Encerrado", EventCategory.SOCIAL,
                    now.minusDays(5), now.minusDays(5).plusHours(4)
            );

            var request = new EventListRequest();

            when(eventRepository.findAll()).thenReturn(List.of(futureEvent, pastEvent));
            when(registrationRepository.countByEventIdAndStatusIn(any(UUID.class), anyCollection()))
                    .thenReturn(0L);

            var result = eventService.list(request);

            assertThat(result.events())
                    .hasSize(1)
                    .extracting("title")
                    .containsExactly("Limpeza do Parque");

            assertThat(result.total()).isEqualTo(1L);
        }

        @Test
        void shouldIncludeEventsEndingExactlyNow() {
            var now = LocalDateTime.now();
            var event = buildEvent(
                    UUID.randomUUID(), "Evento no limite", EventCategory.OTHER,
                    now.minusHours(2), now.plusSeconds(5)
            );

            when(eventRepository.findAll()).thenReturn(List.of(event));
            when(registrationRepository.countByEventIdAndStatusIn(any(UUID.class), anyCollection()))
                    .thenReturn(0L);

            var result = eventService.list(new EventListRequest());

            assertThat(result.events()).hasSize(1);
        }

        @Test
        void shouldReturnEmptyListWhenNoEvents() {
            var request = new EventListRequest();

            when(eventRepository.findAll()).thenReturn(List.of());

            var result = eventService.list(request);

            assertThat(result)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("total", 0L);

            assertThat(result.events()).isEmpty();
        }

        @Test
        void shouldReturnEmptyListWhenAllEventsHaveEnded() {
            var now = LocalDateTime.now();
            var pastEvent = buildEvent(
                    UUID.randomUUID(), "Evento Encerrado", EventCategory.SOCIAL,
                    now.minusDays(10), now.minusDays(9)
            );

            when(eventRepository.findAll()).thenReturn(List.of(pastEvent));

            var result = eventService.list(new EventListRequest());

            assertThat(result.total()).isEqualTo(0L);
            assertThat(result.events()).isEmpty();
        }

        @Test
        void shouldExposeLiveEnrolledCountPerEvent() {
            var now = LocalDateTime.now();
            var event = buildEvent(
                    UUID.randomUUID(), "Mutirão", EventCategory.SOCIAL,
                    now.plusDays(1), now.plusDays(1).plusHours(3)
            );

            when(eventRepository.findAll()).thenReturn(List.of(event));
            when(registrationRepository.countByEventIdAndStatusIn(eq(event.getId()), anyCollection()))
                    .thenReturn(7L);

            var result = eventService.list(new EventListRequest());

            assertThat(result.events()).hasSize(1);
            assertThat(result.events().get(0).enrolledCount()).isEqualTo(7L);
        }
    }

    @Nested
    class ToEventResponseTest {
        @Test
        void shouldMapEventFieldsAndOwnerIdCorrectly() {
            var now = LocalDateTime.now();
            var event = buildEvent(
                    UUID.randomUUID(), "Doação de Alimentos", EventCategory.SOCIAL,
                    now, now.plusHours(2)
            );

            when(registrationRepository.countByEventIdAndStatusIn(eq(event.getId()), anyCollection()))
                    .thenReturn(3L);

            var response = eventService.toEventResponse(event);

            assertThat(response.id()).isEqualTo(event.getId());
            assertThat(response.ownerId()).isEqualTo(event.getOwner().getId());
            assertThat(response.title()).isEqualTo("Doação de Alimentos");
            assertThat(response.category()).isEqualTo(EventCategory.SOCIAL);
            assertThat(response.enrolledCount()).isEqualTo(3L);
            assertThat(response.imageUrl()).isNull();
        }

        @Test
        void shouldEncodeImageAsBase64DataUriWhenPresent() {
            var now = LocalDateTime.now();
            var event = buildEvent(
                    UUID.randomUUID(), "Evento com imagem", EventCategory.OTHER,
                    now, now.plusHours(1)
            );
            event.setImage(new byte[]{1, 2, 3});

            when(registrationRepository.countByEventIdAndStatusIn(eq(event.getId()), anyCollection()))
                    .thenReturn(0L);

            var response = eventService.toEventResponse(event);

            assertThat(response.imageUrl()).startsWith("data:image/*;base64,");
        }
    }

    @Nested
    class RemoveImageTest {
        @Test
        void shouldRemoveImageWhenEventExists() {
            var eventId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            var event = new Event();
            event.setId(eventId);
            event.setImage(new byte[]{1, 2, 3});

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

            var result = eventService.removeImageByEventId(eventId.toString());

            assertThat(result).isTrue();
            assertThat(event.getImage()).isNull();
            verify(eventRepository).save(event);
        }

        @Test
        void shouldReturnFalseWhenEventDoesNotExist() {
            var eventId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            var result = eventService.removeImageByEventId(eventId.toString());

            assertThat(result).isFalse();
            verify(eventRepository, never()).save(any(Event.class));
        }
    }

    @Nested
    class FindByIdTest {
        @Test
        void shouldReturnEventWhenFound() {
            var eventId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            var event = new Event();
            event.setId(eventId);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

            var result = eventService.findById(eventId.toString());

            assertThat(result).isEqualTo(event);
        }

        @Test
        void shouldReturnNullWhenEventDoesNotExist() {
            var eventId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            var result = eventService.findById(eventId.toString());

            assertThat(result).isNull();
        }
    }

    private Event buildEvent(UUID id, String title, EventCategory category, LocalDateTime startsAt, LocalDateTime endsAt) {
        var owner = new User();
        owner.setId(UUID.randomUUID());

        var address = new Address();
        address.setId(UUID.randomUUID());

        var event = new Event();
        event.setId(id);
        event.setTitle(title);
        event.setCategory(category);
        event.setOwner(owner);
        event.setAddress(address);
        event.setStartsAt(startsAt);
        event.setEndsAt(endsAt);
        event.setCapacity(50);
        return event;
    }
}

