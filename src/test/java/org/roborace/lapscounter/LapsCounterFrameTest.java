package org.roborace.lapscounter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.roborace.lapscounter.client.WebsocketClient;
import org.roborace.lapscounter.domain.Message;
import org.roborace.lapscounter.domain.Type;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.roborace.lapscounter.domain.Message.builder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "laps.safe-interval=1000")
class LapsCounterFrameTest extends LapsCounterAbstractTest {

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
    void testFrameIgnoredIfStateNotRunning() throws InterruptedException {

        await().until(() -> robot1.hasMessageWithType(Type.LAP));

        sendFrame(robot1, FIRST_SERIAL, 0xAA00);

        Thread.sleep(safeInterval);

        assertFalse(robot1.hasMessageWithType(Type.LAP));

    }
    @Test
    void testFrameIgnoredFirstSeconds() throws InterruptedException {

        givenRunningState();

        await().until(() -> robot1.hasMessageWithType(Type.LAP));

        sendFrame(robot1, FIRST_SERIAL, 0xAA00);

        Thread.sleep(safeInterval);

        assertFalse(robot1.hasMessageWithType(Type.LAP));

    }

    @Test
    void testFrameSimple() throws InterruptedException {
        givenRunningState();

        Thread.sleep(safeInterval);
        sendFrame(robot1, FIRST_SERIAL, 0xAA00);
        await().untilAsserted(() -> {
            Message lastMessage = shouldReceiveType(robot1, Type.LAP);
            assertThat(lastMessage.getSerial(), equalTo(FIRST_SERIAL));
            assertThat(lastMessage.getLaps(), equalTo(1));
        });

        shouldReceiveType(ui, Type.LAP);

    }

    @Test
    void testFrameOneRobot_RotateNotCounted() throws InterruptedException {
        givenRunningState();

        Thread.sleep(safeInterval);
        sendFrame(robot1, FIRST_SERIAL, 0xAA00);
        await().untilAsserted(() -> {
            Message lastMessage = shouldReceiveType(robot1, Type.LAP);
            assertThat(lastMessage.getSerial(), equalTo(FIRST_SERIAL));
            assertThat(lastMessage.getLaps(), equalTo(1));
        });

        sendFrame(robot1, FIRST_SERIAL, 0xAA00);

        Thread.sleep(safeInterval);

        assertFalse(robot1.hasMessageWithType(Type.LAP));

    }

    @Test
    void testFrameOneRobotSeveralLaps() throws InterruptedException {
        givenRunningState();

        Thread.sleep(safeInterval);

        AtomicInteger laps = new AtomicInteger(0);
        for (int i = 0; i < 3; i++) {
            sendFrame(robot1, FIRST_SERIAL, 0xAA00);
            laps.incrementAndGet();
            await().untilAsserted(() -> {
                Message lastMessage = shouldReceiveType(robot1, Type.LAP);
                assertThat(lastMessage.getSerial(), equalTo(FIRST_SERIAL));
                assertThat(lastMessage.getLaps(), equalTo(laps.get()));
            });
            Thread.sleep(safeInterval);
        }

    }

    private void sendFrame(WebsocketClient robot, int serial, int frame) {
        Message message = builder()
                .type(Type.FRAME)
                .serial(serial)
                .frame(frame)
                .build();
        sendMessage(robot, message);
    }

}