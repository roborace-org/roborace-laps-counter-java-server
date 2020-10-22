package org.roborace.lapscounter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.roborace.lapscounter.client.WebsocketClient;
import org.roborace.lapscounter.domain.Message;
import org.roborace.lapscounter.domain.Type;
import org.roborace.lapscounter.service.Stopwatch;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.roborace.lapscounter.domain.Message.builder;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "laps.safe-interval=100")
class LapsCounterFrameIntegrationTest extends LapsCounterAbstractTest {

    private static final int FRAME_0 = 0xAA00;
    private static final int FRAME_1 = 0xAA01;
    private static final int FRAME_2 = 0xAA02;

    @Value("${laps.safe-interval}")
    private long safeInterval;

    private WebsocketClient robot1;
    private WebsocketClient robot2;


    @BeforeEach
    void setUp() {

        robot1 = createAndInitRobot("ROBOT1", FIRST_SERIAL);
        robot2 = createAndInitRobot("ROBOT2", SECOND_SERIAL);

    }


    @AfterEach
    void tearDown() {
        robot1.closeClient();
        robot2.closeClient();
    }


    @Test
    void testFrameIgnoredIfStateNotRunning() {

        await().until(() -> robot1.hasMessageWithType(Type.LAP));

        sendStartingFrame();

        sleep(safeInterval);

        assertFalse(robot1.hasMessageWithType(Type.LAP));

    }

    @Test
    void testFrameIgnoredFirstSeconds() {

        givenRunningState();

        await().until(() -> robot1.hasMessageWithType(Type.LAP));

        sendFrame(robot1, FIRST_SERIAL, FRAME_0);
        sendFrame(robot1, FIRST_SERIAL, FRAME_1);
        sendFrame(robot1, FIRST_SERIAL, FRAME_2);

        sleep(safeInterval);

        assertFalse(robot1.hasMessageWithType(Type.LAP));

    }

    @Test
    void testFrameSimple() {
        givenRunningState();

        sendStartingFrame();
        sendAllFrames();

        await().untilAsserted(() -> {
            Message lastMessage = shouldReceiveType(robot1, Type.LAP);
            assertThat(lastMessage.getSerial(), equalTo(FIRST_SERIAL));
            assertThat(lastMessage.getLaps(), equalTo(1));
        });

        shouldReceiveType(ui, Type.LAP);

    }

    @Test
    void testFrameWrongRotation() {
        givenRunningState();

        sendAllFramesWrongRotation();

        await().untilAsserted(() -> {
            Message lastMessage = shouldReceiveType(robot1, Type.LAP);
            assertThat(lastMessage.getSerial(), equalTo(FIRST_SERIAL));
            assertThat(lastMessage.getLaps(), equalTo(-1));
        });

        shouldReceiveType(ui, Type.LAP);

    }

    @Test
    void testLastLapTime() {
        givenRunningState();

        sleep(2 * safeInterval);

        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();
        sendStartingFrame();
        sendAllFrames();
        stopwatch.finish();
        await().untilAsserted(() -> {
            Message lastMessage = shouldReceiveType(robot1, Type.LAP);
            assertThat(lastMessage.getSerial(), equalTo(FIRST_SERIAL));
            assertThat(lastMessage.getLaps(), equalTo(1));
            assertTimeEquals(lastMessage.getLastLapTime(), stopwatch.getTime());
        });


        stopwatch.start();
        sleep(2 * safeInterval);
        sendAllFrames();
        stopwatch.finish();
        Message lastMessage = shouldReceiveType(robot1, Type.LAP);
        assertThat(lastMessage.getSerial(), equalTo(FIRST_SERIAL));
        assertThat(lastMessage.getLaps(), equalTo(2));
        assertTimeEquals(lastMessage.getLastLapTime(), stopwatch.getTime());
    }

    @Test
    void testBestLapTime() {
        givenRunningState();

        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();
        sendStartingFrame();
        sleep(2 * safeInterval);
        sendAllFrames();
        stopwatch.finish();
        await().untilAsserted(() -> {
            Message lastMessage = shouldReceiveType(robot1, Type.LAP);
            assertThat(lastMessage.getSerial(), equalTo(FIRST_SERIAL));
            assertThat(lastMessage.getLaps(), equalTo(1));
            assertThat(lastMessage.getLastLapTime(), equalTo(lastMessage.getBestLapTime()));
            assertTimeEquals(lastMessage.getLastLapTime(), stopwatch.getTime());
            assertTimeEquals(lastMessage.getBestLapTime(), stopwatch.getTime());
        });


        stopwatch.start();
        sendAllFrames();
        stopwatch.finish();
        Message lastMessage = shouldReceiveType(robot1, Type.LAP);
        assertThat(lastMessage.getSerial(), equalTo(FIRST_SERIAL));
        assertThat(lastMessage.getLaps(), equalTo(2));
        assertTimeEquals(lastMessage.getLastLapTime(), stopwatch.getTime());
        assertTimeEquals(lastMessage.getBestLapTime(), stopwatch.getTime());
        assertThat(lastMessage.getLastLapTime(), equalTo(lastMessage.getBestLapTime()));

        stopwatch.start();
        sleep(safeInterval);
        sendAllFrames();
        stopwatch.finish();
        lastMessage = shouldReceiveType(robot1, Type.LAP);
        assertThat(lastMessage.getSerial(), equalTo(FIRST_SERIAL));
        assertThat(lastMessage.getLaps(), equalTo(3));
        assertTimeEquals(lastMessage.getLastLapTime(), stopwatch.getTime());
        assertTimeEquals(lastMessage.getBestLapTime(), stopwatch.getTime());
        assertThat(lastMessage.getBestLapTime(), lessThan(lastMessage.getLastLapTime()));
    }

    private void assertTimeEquals(Long time, long expectedTime) {
        assertThat(time, greaterThanOrEqualTo(expectedTime - 100));
        assertThat(time, lessThanOrEqualTo(expectedTime + 100));
    }

    @Test
    void testFrameOneRobotSeveralLaps() {
        givenRunningState();

        sendStartingFrame();

        AtomicInteger laps = new AtomicInteger(0);
        for (int i = 0; i < 3; i++) {
            sendAllFrames();

            laps.incrementAndGet();

            await().untilAsserted(() -> {
                Message lastMessage = shouldReceiveType(robot1, Type.LAP);
                System.out.println("lastMessage = " + lastMessage);
                assertThat(lastMessage.getSerial(), equalTo(FIRST_SERIAL));
                assertThat(lastMessage.getLaps(), equalTo(laps.get()));
            });
        }

    }

    private void sendAllFrames() {
        sendFrames(FRAME_1, FRAME_2, FRAME_0);
    }

    private void sendAllFramesWrongRotation() {
        sendFrames(FRAME_2, FRAME_1, FRAME_0);
    }

    private void sendFrames(Integer... frames) {
        Arrays.stream(frames).forEach(frame -> {
            sleep(2 * safeInterval);
            sendFrame(robot1, FIRST_SERIAL, frame);
        });
    }

    private void sendStartingFrame() {
        sendFrame(robot1, FIRST_SERIAL, FRAME_0);
    }

    private void sendFrame(WebsocketClient robot, int serial, int frame) {
        Message message = builder()
                .type(Type.FRAME)
                .serial(serial)
                .frame(frame)
                .build();
        sendMessage(robot, message);
    }

    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}