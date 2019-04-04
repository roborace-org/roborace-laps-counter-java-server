package org.roborace.lapscounter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.roborace.lapscounter.client.WebsocketClient;
import org.roborace.lapscounter.domain.Message;
import org.roborace.lapscounter.domain.Type;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.roborace.lapscounter.domain.State.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LapsCounterUiTest extends LapsCounterAbstractTest {

    @BeforeEach
    void setUp() {

    }


    @AfterEach
    void tearDown() {

    }

    @Test
    void testHappyPathSimple() {

        sendCommandAndCheckState(STEADY);

        sendCommandAndCheckState(RUNNING);

        sendCommandAndCheckState(FINISH);

    }

    @Test
    void testRestart() {

        sendCommandAndCheckState(STEADY);

        sendCommandAndCheckState(RUNNING);

        sendCommandAndCheckState(FINISH);

        sendCommandAndCheckState(READY);

    }

    @Test
    void testStateUi() {

        sendState();
        shouldReceiveState(ui, READY);

        sendCommand(STEADY);
        shouldReceiveState(ui, STEADY);

        sendState();
        shouldReceiveState(ui, STEADY);

    }

    @Test
    void testSendTime() throws InterruptedException {

        sendCommandAndCheckState(STEADY);

        sendCommandAndCheckState(RUNNING);

        shouldReceiveType(ui, Type.TIME);
        assertThat(ui.getLastMessage().getTime(), lessThan(100L));

        Thread.sleep(TIME_SEND_INTERVAL);
        shouldReceiveType(ui, Type.TIME);
//        assertThat(ui.getLastMessage().getTime(), equalTo(TIME_SEND_INTERVAL));

        sendCommandAndCheckState(FINISH);
        shouldReceiveType(ui, Type.TIME);
        assertThat(ui.getLastMessage().getTime(), greaterThan(TIME_SEND_INTERVAL));
        assertThat(ui.getLastMessage().getTime(), lessThan(TIME_SEND_INTERVAL + 500));

    }

    @Test
    void testWrongCommand() {
        sendMessage(ui, Message.builder().type(Type.COMMAND).build());
        shouldReceiveType(ui, Type.ERROR);
    }

    @Test
    void testWrongOrder() {
        sendCommand(RUNNING);
        shouldReceiveType(ui, Type.ERROR);
        sendCommand(FINISH);
        shouldReceiveType(ui, Type.ERROR);

        sendCommandAndCheckState(STEADY);

        sendCommand(FINISH);
        shouldReceiveType(ui, Type.ERROR);

        sendCommandAndCheckState(RUNNING);

    }

    @Test
    void testLaps() {

        WebsocketClient robot1 = createAndInitRobot("ROBOT1", FIRST_SERIAL);
        assertThat(ui.getLastMessage().getSerial(), equalTo(FIRST_SERIAL));
        assertThat(robot1.getLastMessage().getSerial(), equalTo(FIRST_SERIAL));
        await().until(() -> !robot1.hasMessage());


        WebsocketClient robot2 = createAndInitRobot("ROBOT2", SECOND_SERIAL);
        assertThat(ui.getLastMessage().getSerial(), equalTo(SECOND_SERIAL));
        assertThat(robot2.getLastMessage().getSerial(), equalTo(SECOND_SERIAL));
//        shouldHasNoMessage(robot1); // TODO should not receive this

        Set<Integer> serials = new HashSet<>(Arrays.asList(FIRST_SERIAL, SECOND_SERIAL));
        sendMessage(ui, buildWithType(Type.LAPS));
        await().until(() -> {
            shouldReceiveType(ui, Type.LAP);
            System.out.println("serials = " + serials);
            System.out.println("ui.getLastMessage().getSerial() = " + ui.getLastMessage().getSerial());
            assertThat(serials, hasItem(ui.getLastMessage().getSerial()));
            serials.remove(ui.getLastMessage().getSerial());
            return serials.isEmpty();
        });


    }

}