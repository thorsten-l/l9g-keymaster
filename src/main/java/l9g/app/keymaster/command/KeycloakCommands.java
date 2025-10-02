/*
 * Copyright 2024 Thorsten Ludewig (t.ludewig@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package l9g.app.keymaster.command;

import l9g.app.keymaster.service.KeycloakService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@RequiredArgsConstructor
@Command(group = "Keycloak")
@Slf4j
public class KeycloakCommands
{

  private final KeycloakService keycloakService;

  @Command(description = "delete realm roles with null description")
  public void deleteRealmRolesWithNullDescription()
    throws Throwable
  {
    keycloakService.deleteRealmRolesWithNullDescription();
  }

  @Command(description = "users in selected realm")
  public void listUsers()
    throws Throwable
  {
    keycloakService.listUsers(true);
  }

  @Command(description = "list realms")
  public void listRealms()
    throws Throwable
  {
    keycloakService.realms();
  }

  @Command(description = "list client scopes")
  public void listClientScopes()
    throws Throwable
  {
    keycloakService.clientScopes();
  }

  @Command(description = "list realm roles")
  public void listRealmRoles()
    throws Throwable
  {
    keycloakService.listRealmRoles();
  }

  @Command(description = "list all clients")
  public void listClients()
    throws Throwable
  {
    log.debug("list all clients");
    keycloakService.listClients();
  }
  
  @Command(description = "list client roles")
  public void listClientRoles(
    @Option(description = "client name", required = true) String clientName)
    throws Throwable
  {
    keycloakService.listClientRoles(clientName);
  }

  @Command(description = "show user details by username")
  public void showUser(
    @Option(description = "Username", required = true) String userName)
    throws Throwable
  {
    keycloakService.showUser(userName);
  }

  @Command(description = "show user details by ID")
  public void showUserById(
    @Option(description = "User ID (UID)", required = true) String userId)
    throws Throwable
  {
    keycloakService.showUser(userId);
  }

  @Command(description = "show client details")
  public void showClient(
    @Option(description = "client name", required = true) String clientName)
    throws Throwable
  {
    keycloakService.showClient(clientName);
  }
}
