package com.example.KW_SPACE.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.KW_SPACE.classroom.domain.Classroom;
import com.example.KW_SPACE.classroom.domain.ClassroomRepository;
import com.example.KW_SPACE.reservation.domain.ReservationRepository;
import com.example.KW_SPACE.reservation.domain.ReservationStatus;
import com.example.KW_SPACE.reservation.dto.ReservationCreateRequest;
import com.example.KW_SPACE.reservation.exception.ReservationErrorCode;
import com.example.KW_SPACE.reservation.exception.ReservationException;
import com.example.KW_SPACE.reservation.service.ReservationService;
import com.example.KW_SPACE.user.domain.User;
import com.example.KW_SPACE.user.domain.UserRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ReservationConcurrencyTest {

	private static final LocalDate DATE = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(1);
	private static final LocalTime SLOT_START = LocalTime.of(9, 0);
	private static final LocalTime SLOT_END = LocalTime.of(10, 30);

	@Autowired
	private ReservationService reservationService;

	@Autowired
	private ClassroomRepository classroomRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ReservationRepository reservationRepository;

	private Long classroomId;
	private Long userId;

	@BeforeEach
	void setUp() {
		reservationRepository.deleteAll();
		classroomRepository.deleteAll();
		userRepository.deleteAll();

		classroomId = classroomRepository.save(Classroom.create(1, "101")).getId();
		userId = userRepository.save(User.create("2025404000", "이효원", null, "encoded-password")).getId();
	}

	@AfterEach
	void tearDown() {
		reservationRepository.deleteAll();
		classroomRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void onlyOneSucceedsWhenTwoReserveSameSlotConcurrently() throws Exception {
		int threads = 2;
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		CountDownLatch ready = new CountDownLatch(threads);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threads);
		AtomicInteger success = new AtomicInteger();
		AtomicInteger conflict = new AtomicInteger();

		Runnable task = () -> {
			ready.countDown();
			try {
				start.await();
				reservationService.create(userId,
						new ReservationCreateRequest(classroomId, DATE, SLOT_START, SLOT_END));
				success.incrementAndGet();
			} catch (ReservationException exception) {
				if (exception.getErrorCode() == ReservationErrorCode.RESERVATION_CONFLICT) {
					conflict.incrementAndGet();
				}
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
			} finally {
				done.countDown();
			}
		};

		for (int i = 0; i < threads; i++) {
			executor.submit(task);
		}
		ready.await();
		start.countDown();
		done.await(10, TimeUnit.SECONDS);
		executor.shutdownNow();

		assertThat(success.get()).isEqualTo(1);
		assertThat(conflict.get()).isEqualTo(1);
		assertThat(reservationRepository.findByClassroomIdAndDateAndStatus(
				classroomId, DATE, ReservationStatus.RESERVED)).hasSize(1);
	}
}
