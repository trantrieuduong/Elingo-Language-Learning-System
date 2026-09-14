package com.elingo;

import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages("com.elingo")
@IncludeTags("unit")
public class UnitTestSuite {
}
