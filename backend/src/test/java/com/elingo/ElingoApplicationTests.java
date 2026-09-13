package com.elingo;

import com.elingo.user.UserIntegrationTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        UserIntegrationTest.class
})
public class ElingoApplicationTests {
}
