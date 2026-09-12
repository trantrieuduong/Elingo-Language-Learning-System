package com.elingo;

import com.elingo.user.controller.UserControllerIntegrationTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        UserControllerIntegrationTest.class
})
public class ElingoApplicationTests {
}
