package org.roborace.lapscounter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.roborace.lapscounter.client.WebsocketClient;
import org.roborace.lapscounter.domain.Message;
import org.roborace.lapscounter.domain.Type;
import org.roborace.lapscounter.service.LapsCounterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.roborace.lapscounter.domain.State.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LapsCounterUiIntegrationTest extends LapsCounterAbstractTest {

    public static final long PIT_STOP_TEST_TIME = 1230;

    @Autowired
    private LapsCounterService lapsCounterService;

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(lapsCounterService, "pitStopTime", PIT_STOP_TEST_TIME);

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
        assertThat(ui.getLastMessage().getRaceTimeLimit(), is(0L));

        Thread.sleep(TIME_SEND_INTERVAL);
        shouldReceiveType(ui, Type.TIME);
//        assertThat(ui.getLastMessage().getTime(), equalTo(TIME_SEND_INTERVAL));

        sendCommandAndCheckState(FINISH);
        shouldReceiveType(ui, Type.TIME);
        assertThat(ui.getLastMessage().getTime(), greaterThan(TIME_SEND_INTERVAL));
        assertThat(ui.getLastMessage().getTime(), lessThan(TIME_SEND_INTERVAL + 500));

    }

    @Test
    void testSendRaceTimeLimit() {

        shouldReceiveType(ui, Type.TIME);
        assertThat(ui.getLastMessage().getTime(), is(0L));
        assertThat(ui.getLastMessage().getRaceTimeLimit(), is(0L));

        sendTimeRequestCommand(3600L);

        shouldReceiveType(ui, Type.TIME);
        assertThat(ui.getLastMessage().getTime(), is(0L));
        assertThat(ui.getLastMessage().getRaceTimeLimit(), is(3600L));

    }

    @Test
    void testAutoFinishRaceByTimeLimit() {

        long raceTimeLimit = 3L;
        sendTimeRequestCommand(raceTimeLimit);
        sendCommandAndCheckState(STEADY);

        sendCommandAndCheckState(RUNNING);

        shouldReceiveState(ui, FINISH);

        shouldReceiveType(ui, Type.TIME);
        assertThat(ui.getLastMessage().getTime(), greaterThanOrEqualTo(raceTimeLimit * 1000L));
        assertThat(ui.getLastMessage().getTime(), lessThan(raceTimeLimit * 1000L + 100));
        assertThat(ui.getLastMessage().getRaceTimeLimit(), is(raceTimeLimit));

    }

    @Test
    void testReceivePitStopFinish() {
        WebsocketClient robot1 = createAndInitRobot("ROBOT1", FIRST_SERIAL);
        givenRunningState();

        sendMessage(robot1, Message.builder().serial(FIRST_SERIAL).type(Type.PIT_STOP).build());

        shouldReceiveType(ui, Type.PIT_STOP_FINISH);
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
        await().until(robot1::hasNoMessage);


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