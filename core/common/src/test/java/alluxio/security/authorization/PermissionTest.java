/*
 * The Alluxio Open Foundation licenses this work under the Apache License, version 2.0
 * (the "License"). You may not use this work except in compliance with the License, which is
 * available at www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied, as more fully set forth in the License.
 *
 * See the NOTICE file distributed with this work for information regarding copyright ownership.
 */

package alluxio.security.authorization;

import alluxio.Configuration;
import alluxio.Constants;
import alluxio.security.LoginUser;
import alluxio.security.authentication.AuthType;
import alluxio.security.authentication.AuthenticatedClientUser;
import alluxio.security.group.GroupMappingService;
import alluxio.security.group.provider.IdentityUserGroupsMapping;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Tests the {@link Permission} class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({GroupMappingService.Factory.class})
public final class PermissionTest {

  /**
   * The exception expected to be thrown.
   */
  @Rule
  public ExpectedException mThrown = ExpectedException.none();

  /**
   * Tests the {@link Permission#applyUMask(Mode)} method.
   */
  @Test
  public void applyUMaskTest() {
    Mode umaskMode = new Mode((short) 0022);
    Permission permission = new Permission("user1", "group1", Mode.getDefault());
    permission.applyUMask(umaskMode);

    Assert.assertEquals(Mode.Bits.ALL, permission.getMode().getUserBits());
    Assert.assertEquals(Mode.Bits.READ_EXECUTE, permission.getMode().getGroupBits());
    Assert.assertEquals(Mode.Bits.READ_EXECUTE, permission.getMode().getOtherBits());
    verifyPermission("user1", "group1", (short) 0755, permission);
  }

  /**
   * Tests the {@link Permission#defaults()} method.
   */
  @Test
  public void defaultsTest() throws Exception {
    Configuration conf = new Configuration();

    // no authentication
    conf.set(Constants.SECURITY_AUTHENTICATION_TYPE, AuthType.NOSASL.getAuthName());
    Permission permission = Permission.defaults();
    verifyPermission("", "", (short) 0777, permission);
  }

  /**
   * Tests the {@link Permission#setUserFromThriftClient(Configuration)} method.
   */
  @Test
  public void setUserFromThriftClientTest() throws Exception {
    Configuration conf = new Configuration();
    Permission permission = Permission.defaults();

    // When security is not enabled, user and group are not set
    conf.set(Constants.SECURITY_AUTHENTICATION_TYPE, AuthType.NOSASL.getAuthName());
    permission.setUserFromThriftClient(conf);
    verifyPermission("", "", (short) 0777, permission);

    conf.set(Constants.SECURITY_AUTHENTICATION_TYPE, AuthType.SIMPLE.getAuthName());
    conf.set(Constants.SECURITY_GROUP_MAPPING, IdentityUserGroupsMapping.class.getName());
    AuthenticatedClientUser.set("test_client_user");

    // When authentication is enabled, user and group are inferred from thrift transport
    permission.setUserFromThriftClient(conf);
    verifyPermission("test_client_user", "test_client_user", (short) 0777, permission);
  }

  /**
   * Tests the {@link Permission#setUserFromLoginModule(Configuration)} method.
   */
  @Test
  public void setUserFromLoginModuleTest() throws Exception {
    Configuration conf = new Configuration();
    Permission permission = Permission.defaults();

    // When security is not enabled, user and group are not set
    conf.set(Constants.SECURITY_AUTHENTICATION_TYPE, AuthType.NOSASL.getAuthName());
    permission.setUserFromThriftClient(conf);
    verifyPermission("", "", (short) 0777, permission);

    // When authentication is enabled, user and group are inferred from login module
    conf.set(Constants.SECURITY_AUTHENTICATION_TYPE, AuthType.SIMPLE.getAuthName());
    conf.set(Constants.SECURITY_LOGIN_USERNAME, "test_login_user");
    conf.set(Constants.SECURITY_GROUP_MAPPING, IdentityUserGroupsMapping.class.getName());
    Whitebox.setInternalState(LoginUser.class, "sLoginUser", (String) null);

    permission.setUserFromLoginModule(conf);
    verifyPermission("test_login_user", "test_login_user", (short) 0777, permission);
  }

  private void verifyPermission(String user, String group, short mode, Permission permission) {
    Assert.assertEquals(user, permission.getUserName());
    Assert.assertEquals(group, permission.getGroupName());
    Assert.assertEquals(mode, permission.getMode().toShort());
  }
}
