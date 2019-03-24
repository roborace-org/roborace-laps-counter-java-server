package org.roborace.lapscounter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.roborace.lapscounter.client.WebsocketClient;
import org.roborace.lapscounter.domain.Message;
import org.roborace.lapscounter.domain.Type;
import org.springframework.boot.test.context.SpringBootTest;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.roborace.lapscounter.domain.Message.builder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LapsCounterFrameTest extends LapsCounterAbstractTest {

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
    void testFrameOneRobot() {

        sendMessage(robot1, builder().type(Type.FRAME).serial(FIRST_SERIAL).frame(0xAA00).build());
        await().untilAsserted(() -> {
            Message lastMessage = shouldReceiveType(robot1, Type.LAP);
            assertThat(lastMessage.getSerial(), equalTo(FIRST_SERIAL));
            assertThat(lastMessage.getLaps(), equalTo(1));
        });

    }

}