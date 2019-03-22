package org.roborace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.roborace.Message.builder;
import static org.roborace.State.FINISH;
import static org.roborace.State.READY;
import static org.roborace.State.RUNNING;
import static org.roborace.State.STEADY;

class LapsCounterRobotTest extends LapsCounterAbstractTest {

    private static final int MAX_ROBOTS = 8;
    private WebsocketClient robot1;


    @BeforeEach
    void setUp() {

        robot1 = createClient("ROBOT1");
        shouldReceiveState(robot1, READY);

    }


    @AfterEach
    void tearDown() {
        robot1.closeClient();
    }


    @Test
    void testStateRobot() {

        sendCommandAndCheckState(STEADY);
        shouldReceiveState(robot1, STEADY);

        sendCommandAndCheckState(RUNNING);
        shouldReceiveState(robot1, RUNNING);

        sendCommandAndCheckState(FINISH);
        shouldReceiveState(robot1, FINISH);

    }

    @Test
    void testRobotInit() {

        sendMessage(robot1, builder().type(Type.ROBOT_INIT).serial(100).build());

        shouldReceiveType(robot1, Type.LAP);
        assertThat(robot1.getLastMessage().getSerial(), equalTo(100));

        shouldReceiveType(ui, Type.LAP);
        assertThat(ui.getLastMessage().getSerial(), equalTo(100));

    }

    @Test
    void testRobotEdit() {

        sendMessage(robot1, builder().type(Type.ROBOT_INIT).serial(100).build());

        shouldReceiveType(robot1, Type.LAP);
        assertThat(robot1.getLastMessage().getSerial(), equalTo(100));

        shouldReceiveType(ui, Type.LAP);
        assertThat(ui.getLastMessage().getSerial(), equalTo(100));

        String newName = "WINNER " + new Random().nextInt(100);
        sendMessage(ui, builder().type(Type.ROBOT_EDIT).serial(100).name(newName).build());

        shouldReceiveType(ui, Type.LAP);
        assertThat(ui.getLastMessage().getSerial(), equalTo(100));
        assertThat(ui.getLastMessage().getName(), equalTo(newName));


        sendMessage(ui, buildWithType(Type.LAPS));
        shouldReceiveType(ui, Type.LAP);
        assertThat(ui.getLastMessage().getSerial(), equalTo(100));
        assertThat(ui.getLastMessage().getName(), equalTo(newName));

    }

    @Test
    void testMaxRobots() {
        robot1.closeClient();

        List<WebsocketClient> robots = IntStream.range(0, MAX_ROBOTS)
                .mapToObj(i -> createClient("R" + i))
                .collect(Collectors.toList());

        robots.forEach(robot -> shouldReceiveState(robot, READY));

        robots.forEach(robot -> {
            int code = 100 + robot.getName().charAt(1) - '0';
            sendMessage(robot, builder().type(Type.ROBOT_INIT).serial(code).build());
            shouldReceiveLap(robots);
        });

        sendCommandAndCheckState(STEADY);
        robots.forEach(robot -> shouldReceiveState(robot, STEADY));

        sendCommandAndCheckState(RUNNING);
        robots.forEach(robot -> shouldReceiveState(robot, RUNNING));

        sendCommandAndCheckState(FINISH);
        robots.forEach(robot -> shouldReceiveState(robot, FINISH));


        robots.forEach(WebsocketClient::closeClient);
    }

    private void shouldReceiveLap(List<WebsocketClient> robots) {
        shouldReceiveType(ui, Type.LAP);
        robots.forEach(robot -> shouldReceiveType(robot, Type.LAP));
    }


}