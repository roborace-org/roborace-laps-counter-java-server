package org.roborace.lapscounter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.roborace.lapscounter.client.WebsocketClient;
import org.roborace.lapscounter.domain.Type;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.roborace.lapscounter.domain.Message.builder;
import static org.roborace.lapscounter.domain.State.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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

        sendMessage(robot1, builder().type(Type.ROBOT_INIT).serial(FIRST_SERIAL).build());

        shouldReceiveType(robot1, Type.LAP);
        assertThat(robot1.getLastMessage().getSerial(), equalTo(FIRST_SERIAL));

        shouldReceiveType(ui, Type.LAP);
        assertThat(ui.getLastMessage().getSerial(), equalTo(FIRST_SERIAL));

    }

    @Test
    void testRobotEdit() {

        sendMessage(robot1, builder().type(Type.ROBOT_INIT).serial(FIRST_SERIAL).build());

        shouldReceiveType(robot1, Type.LAP);
        assertThat(robot1.getLastMessage().getSerial(), equalTo(FIRST_SERIAL));

        shouldReceiveType(ui, Type.LAP);
        assertThat(ui.getLastMessage().getSerial(), equalTo(FIRST_SERIAL));

        String newName = "WINNER " + new Random().nextInt(FIRST_SERIAL);
        sendMessage(ui, builder().type(Type.ROBOT_EDIT).serial(FIRST_SERIAL).name(newName).build());

        shouldReceiveType(ui, Type.LAP);
        assertThat(ui.getLastMessage().getSerial(), equalTo(FIRST_SERIAL));
        assertThat(ui.getLastMessage().getName(), equalTo(newName));


        sendMessage(ui, buildWithType(Type.LAPS));
        shouldReceiveType(ui, Type.LAP);
        assertThat(ui.getLastMessage().getSerial(), equalTo(FIRST_SERIAL));
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
            int code = FIRST_SERIAL + robot.getName().charAt(1) - '0';
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