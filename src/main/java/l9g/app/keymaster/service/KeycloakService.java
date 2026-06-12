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
package l9g.app.keymaster.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import l9g.app.keymaster.command.SystemCommands;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Service
@Slf4j
public class KeycloakService
{
  private final String realm;

  private final Keycloak keycloak;

  private final ClientService clientService;

  private final RealmRoleService realmRoleService;

  private final UserService userService;

  public KeycloakService(
    KeycloakBuilderService keycloakBuilderService,
    ClientService clientService,
    RealmRoleService realmRoleService,
    UserService userService
  )
  {
    System.out.println("\n" + SystemCommands.GREETING + "\n");
    this.realm = keycloakBuilderService.getRealm();
    this.keycloak = keycloakBuilderService.getKeycloak();
    this.clientService = clientService;
    this.realmRoleService = realmRoleService;
    this.userService = userService;
  }

  public List<UserRepresentation> searchByUsername(String username,
    boolean exact)
  {
    log.info("Searching by username: {} (exact {})", username, exact);
    List<UserRepresentation> users = keycloak.realm(realm)
      .users()
      .searchByUsername(username, exact);

    log.info("Users found by username {}", users.stream()
      .map(user -> user.getUsername())
      .collect(Collectors.toList()));

    return users;
  }

  public List<UserRepresentation> searchByRole(String roleName)
  {
    log.info("Searching by role: {}", roleName);

    List<UserRepresentation> users = keycloak.realm(realm)
      .roles()
      .get(roleName)
      .getUserMembers();

    log.info("Users found by role {}", users.stream()
      .map(user -> user.getUsername())
      .collect(Collectors.toList()));
    return users;
  }

  public List<RealmRepresentation> realms()
  {
    List<RealmRepresentation> realmsList = keycloak.realms().findAll();
    log.debug("{} realms", realmsList.size());
    realmsList.forEach(realm ->
    {
      System.out.println(realm.getId() + " " + realm.getRealm() + ", " + realm.getDisplayName());
    });
    return realmsList;
  }

  public List<ClientScopeRepresentation> clientScopes()
  {
    log.debug("realm={}", realm);

    List<ClientScopeRepresentation> scopesList = keycloak.realm(realm).clientScopes().findAll();
    log.debug("{} scopes", scopesList.size());
    scopesList.forEach(scope ->
    {
      System.out.println("\n" + scope.getId() + " " + scope.getName() + ", " + scope.getProtocol());
      scope.getAttributes().forEach((key, value) ->
      {
        System.out.println("  - " + key + " = " + value);
      });

      if(scope.getProtocolMappers() != null)
      {
        scope.getProtocolMappers().listIterator().forEachRemaining(mapper ->
        {
          System.out.println("    > " + mapper.getName() + ", " + mapper.getProtocolMapper());
          mapper.getConfig().forEach((key, value) ->
          {
            System.out.println("      * " + key + " = " + value);
          });
        });
      }
      else
      {
        System.out.println("    > none");
      }
    });

    return scopesList;
  }

  public List<UserRepresentation> listUsers(boolean debug)
    throws IOException
  {
    log.debug("listUsers");
    double start = System.currentTimeMillis();

    List<UserRepresentation> allUsers = userService.users();

    if(debug)
    {
      allUsers.forEach(entry ->
      {
        log.debug(entry.getId() + " " + entry.getUsername() + ", " + entry.
          getFederationLink() + ", " + entry.getFirstName() + ", " + entry.
          getLastName());
      });
    }

    return allUsers;
  }

  public List<RoleRepresentation> listRealmRoles()
  {
    log.debug("realm={}", realm);

    List<RoleRepresentation> rolesList = realmRoleService.roles();
    log.debug("{} realm roles", rolesList.size());

    rolesList.stream()
      .sorted((r1, r2) -> r1.getName().compareToIgnoreCase(r2.getName()))
      .forEach(role ->
      {
        System.out.println("'" + role.getName() + "', "
          + role.getDescription() + " (" + role.getId() + ")");

        Set<RoleRepresentation> composites = realmRoleService.compositesByName(role.getName());

        if(composites != null &&  ! composites.isEmpty())
        {
          System.out.println("    Associated Roles:");
          composites.stream()
            .sorted((r1, r2) -> r1.getName().compareToIgnoreCase(r2.getName()))
            .forEach(composite ->
            {
              if(composite.getClientRole())
              {

                String containerId = composite.getContainerId();
                ClientRepresentation result = clientService.clientById(containerId);

                if(result != null)
                {
                  containerId = result.getClientId();
                }

                System.out.println("      - [" + containerId + "] " + composite.getName());
              }
              else
              {
                System.out.println("      - " + composite.getName());
              }
            });
        }

      });
    return rolesList;
  }

  public List<RoleRepresentation> listClientRoles(String clientName)
    throws IOException
  {
    log.info("Listing client roles for client: {}", clientName);
    List<ClientRepresentation> clients = keycloak.realm(realm).clients().findByClientId(clientName);

    if(clients.isEmpty())
    {
      System.out.println("Client with name '" + clientName + "' not found.");
      return new ArrayList<>();
    }

    // Assuming the first client found is the correct one, or client IDs are unique
    ClientRepresentation client = clients.get(0);
    String clientId = client.getId();

    List<RoleRepresentation> clientRoles = keycloak.realm(realm).clients().get(clientId).roles().list();

    if(clientRoles.isEmpty())
    {
      System.out.println("No client roles found for client '" + clientName + "'.");
    }
    else
    {
      System.out.println("Client roles for '" + clientName + "':");
      clientRoles.stream()
        .sorted((r1, r2) -> r1.getName().compareToIgnoreCase(r2.getName()))
        .forEach(role ->
        {
          System.out.println("  - " + role.getName() + " (ID: " + role.getId() + ")");

          List<String> realmRoleUsage = findClientRoleUsageInRealmRoles(role.getName(), clientId);
          if( ! realmRoleUsage.isEmpty())
          {
            System.out.println("    Used as composite in realm roles: " + String.join(", ", realmRoleUsage));
          }

          try
          {
            List<String> userUsage = findClientRoleUsageInUsers(role.getName(), clientId);
            if( ! userUsage.isEmpty())
            {
              System.out.println("    Assigned to users: " + String.join(", ", userUsage));
            }
          }
          catch(IOException ex)
          {
            System.getLogger(KeycloakService.class.getName()).log(System.Logger.Level.ERROR, (String)null, ex);
          }
        });

    }
    return clientRoles;
  }

  public UserRepresentation usersCreate(UserRepresentation user)
  {
    log.info("Creating user: {}", user.getUsername());
    try(Response response = keycloak.realm(realm).users().create(user))
    {
      if(response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL)
      {
        System.out.println("User '" + user.getUsername() + "' created successfully.");
        // Keycloak does not return the full UserRepresentation on create, only the location header
        // To get the full representation, we would need to parse the location header for the ID
        // and then fetch the user. For simplicity, we'll just return the input user for now.
        return user;
      }
      else
      {
        String errorMessage = response.readEntity(String.class);
        System.err.println("Failed to create user '" + user.getUsername() + "': " + errorMessage);
        log.error("Failed to create user {}: {}", user.getUsername(), errorMessage);
        return null;
      }
    }
    catch(Exception e)
    {
      System.err.println("Error creating user '" + user.getUsername() + "': " + e.getMessage());
      log.error("Error creating user {}: {}", user.getUsername(), e.getMessage());
      return null;
    }
  }

  public UserRepresentation usersUpdate(String id, UserRepresentation user)
  {
    log.info("Updating user with ID: {}", id);
    try
    {
      keycloak.realm(realm).users().get(id).update(user);
      System.out.println("User with ID '" + id + "' updated successfully.");
      return user;
    }
    catch(Exception e)
    {
      System.err.println("Error updating user with ID '" + id + "': " + e.getMessage());
      log.error("Error updating user with ID {}: {}", id, e.getMessage());
      return null;
    }
  }

  public boolean usersDelete(String id)
  {
    log.debug("delete user {}", id);

    try
    {
      System.out.
        println(keycloak.tokenManager().getAccessToken().getExpiresIn());
      Response response = keycloak.realm(realm).users().delete(id);
      log.debug("---", id);

      System.out.println(response.getStatus());
    }
    catch(Throwable t)
    {
      log.error("*** delete failed", t);
    }

    log.debug("---", id);

    return false;
  }

  public List<String> deleteRealmRolesWithNullDescription()
  {
    log.info("Deleting realm roles with null description in realm: {}", realm);
    List<String> deletedRoleNames = new ArrayList<>();

    List<RoleRepresentation> roles = keycloak.realm(realm).roles().list();

    for(RoleRepresentation role : roles)
    {
      if(role.getDescription() == null) // && role.getName().charAt(0) == role.getName().toLowerCase().charAt(0))
      {
        try
        {
          log.info("Deleting role: {} (ID: {}) with null description", role.getName(), role.getId());
          keycloak.realm(realm).roles().get(role.getName()).remove();
          deletedRoleNames.add(role.getName());
          System.out.println("Deleted role: " + role.getName());
        }
        catch(Exception e)
        {
          log.error("Failed to delete role {} (ID: {}): {}", role.getName(), role.getId(), e.getMessage());
        }
      }
    }

    if(deletedRoleNames.isEmpty())
    {
      System.out.println("No realm roles with null description found.");
    }
    else
    {
      System.out.println("Successfully deleted roles: " + String.join(", ", deletedRoleNames));
    }
    return deletedRoleNames;
  }

  public void listClients()
  {
    log.debug("Listing all clients in realm: {}", realm);
    Collection<ClientRepresentation> clients = clientService.clients();

    if(clients.isEmpty())
    {
      System.out.println("No clients found in realm '" + realm + "'.");
    }
    else
    {
      System.out.println("\nClients in realm '" + realm + "':");
      clients.stream()
        .sorted((c1, c2) -> c1.getClientId().compareToIgnoreCase(c2.getClientId()))
        .forEach(client ->
        {
          System.out.println("  - " + client.getClientId() + " (ID: " + client.getId() + ", Enabled: " + client.isEnabled() + ")");
        });
    }
  }

  private List<String> findClientRoleUsageInRealmRoles(String clientRoleName, String clientId)
  {
    log.debug("findClientRoleUsageInRealmRoles");
    List<String> usageList = new ArrayList<>();
    List<RoleRepresentation> realmRoles = realmRoleService.roles();
    for(RoleRepresentation realmRole : realmRoles)
    {
      // Get the RoleResource for the realm role
      RoleResource roleResource = realmRoleService.resourceByName(realmRole.getName());

      Set<RoleRepresentation> compositeRoles = new HashSet<>();
      compositeRoles.addAll(roleResource.getRealmRoleComposites());
      compositeRoles.addAll(roleResource.getClientRoleComposites(clientId));

      for(RoleRepresentation compositeRole : compositeRoles)
      {
        if(compositeRole.getName().equals(clientRoleName) && compositeRole.getClientRole() && compositeRole.getContainerId().equals(clientId))
        {
          usageList.add(realmRole.getName());
          break;
        }
      }
    }
    return usageList;
  }

  private List<String> findClientRoleUsageInUsers(String clientRoleName, String clientId)
    throws IOException
  {
    log.debug("findClientRoleUsageInUsers");
    List<String> usageList = new ArrayList<>();
    
    List<UserRepresentation> users = listUsers(log.isDebugEnabled());
    log.debug("{} users found", users.size());
    double start = System.currentTimeMillis();
    for(UserRepresentation user : users)
    {
      List<RoleRepresentation> userClientRoles = keycloak.realm(realm).users().get(user.getId()).roles().clientLevel(clientId).listAll();
 
      for(RoleRepresentation userClientRole : userClientRoles)
      {
        if(userClientRole.getName().equals(clientRoleName))
        {
          usageList.add(user.getUsername());
        }
      }
    }
    double end = System.currentTimeMillis();
    log.debug("role check in {}s", ((end - start) / 1000.0));

    return usageList;
  }

  public void showUser(String userName)
  {
    log.info("Showing details for user name: {}", userName);
    List<UserRepresentation> result = keycloak.realm(realm).users().searchByUsername(userName, Boolean.TRUE);
    if(result.isEmpty())
    {
      System.out.println("No user found");
    }
    else
    {
      showUserById(result.get(0).getId());
    }
  }

  public void showUserById(String userId)
  {
    log.info("Showing details for user ID: {}", userId);
    try
    {

      UserRepresentation user = keycloak.realm(realm).users().get(userId).toRepresentation();
      if(user == null)
      {
        System.out.println("User with ID '" + userId + "' not found.");
        return;
      }

      System.out.println("\nUser Details for ID: " + userId);
      System.out.println("-----------------------------------");
      System.out.println("Username       : " + user.getUsername());
      System.out.println("First Name     : " + user.getFirstName());
      System.out.println("Last Name      : " + user.getLastName());
      System.out.println("Enabled        : " + user.isEnabled());
      System.out.println("Email          : " + user.getEmail());
      System.out.println("Email Verified : " + user.isEmailVerified());

      // Attributes
      if(user.getAttributes() != null &&  ! user.getAttributes().isEmpty())
      {
        System.out.println("\nAttributes:");
        user.getAttributes().forEach((key, value) -> System.out.println("  - '" + key + "' = " + value));
      }

      // Credentials
      List<CredentialRepresentation> credentials = keycloak.realm(realm).users().get(userId).credentials();
      if(credentials != null &&  ! credentials.isEmpty())
      {
        DateTimeFormatter formatter = DateTimeFormatter
          .ofPattern("yyyy-MM-dd HH:mm:ss")
          .withZone(ZoneId.systemDefault());

        System.out.println("\nCredentials:");
        credentials.stream()
          .sorted((c1, c2) -> c1.getType().compareToIgnoreCase(c2.getType()))
          .forEach(credential ->
          {
            String created = credential.getCreatedDate() != null
              ? formatter.format(Instant.ofEpochMilli(credential.getCreatedDate()))
              : "n/a";
            String label = credential.getUserLabel() != null
              ? credential.getUserLabel() : "";
            System.out.println("  - Type: " + credential.getType()
              + ", Label: '" + label + "', Created: " + created);
          });
      }

      // Realm Roles
      List<RoleRepresentation> realmRoles = keycloak.realm(realm).users().get(userId).roles().realmLevel().listAll();
      if( ! realmRoles.isEmpty())
      {
        System.out.println("\nRealm Roles:");
        realmRoles.stream()
          .sorted((r1, r2) -> r1.getName().compareToIgnoreCase(r2.getName()))
          .forEach(role -> System.out.println("  - '" + role.getName() + "'"));
      }

      // Client Roles
      List<ClientRepresentation> clients = keycloak.realm(realm).clients().findAll();
      if(clients != null &&  ! clients.isEmpty())
      {
        System.out.println("\nClient Roles:");
        clients.stream()
          .sorted((c1, c2) -> c1.getClientId().compareToIgnoreCase(c2.getClientId()))
          .forEach(client ->
          {
            List<RoleRepresentation> clientRoles = keycloak.realm(realm).users().get(userId).roles().clientLevel(client.getId()).listAll();
            if( ! clientRoles.isEmpty())
            {
              System.out.println("  Client: " + client.getClientId());
              clientRoles.stream()
                .sorted((r1, r2) -> r1.getName().compareToIgnoreCase(r2.getName()))
                .forEach(role -> System.out.println("    - '" + role.getName() + "'"));
            }
          });
      }

      System.out.println();
    }
    catch(Exception e)
    {
      System.err.println("Error showing user details for ID '" + userId + "': " + e.getMessage());
      log.error("Error showing user details for ID {}: {}", userId, e.getMessage());
    }
  }

  public void removeUserCredentialsExceptPassword(String username)
  {
    log.info("Removing non-password credentials for user: {}", username);

    List<UserRepresentation> result = keycloak.realm(realm)
      .users().searchByUsername(username, Boolean.TRUE);

    if(result.isEmpty())
    {
      System.out.println("No user found with username '" + username + "'.");
      return;
    }

    String userId = result.get(0).getId();
    List<CredentialRepresentation> credentials = keycloak.realm(realm)
      .users().get(userId).credentials();

    if(credentials == null || credentials.isEmpty())
    {
      System.out.println("User '" + username + "' has no credentials.");
      return;
    }

    int removed = 0;
    for(CredentialRepresentation credential : credentials)
    {
      if(CredentialRepresentation.PASSWORD.equalsIgnoreCase(credential.getType()))
      {
        continue;
      }

      try
      {
        keycloak.realm(realm).users().get(userId).removeCredential(credential.getId());
        System.out.println("Removed credential: type=" + credential.getType()
          + ", label='" + (credential.getUserLabel() != null ? credential.getUserLabel() : "")
          + "', id=" + credential.getId());
        removed++;
      }
      catch(Exception e)
      {
        System.err.println("Failed to remove credential " + credential.getId()
          + " (" + credential.getType() + "): " + e.getMessage());
        log.error("Failed to remove credential {} for user {}: {}",
          credential.getId(), username, e.getMessage());
      }
    }

    System.out.println("Removed " + removed + " credential(s) for user '" + username + "'.");
  }

  public void exportRealm(String fileName)
  {
    log.info("Exporting realm '{}' to file '{}'", realm, fileName);

    if(fileName == null || fileName.isBlank())
    {
      System.err.println("Filename must not be empty.");
      return;
    }

    try
    {
      RealmRepresentation realmRepresentation = keycloak.realm(realm)
        .partialExport(true, true);

      ObjectMapper mapper = new ObjectMapper();
      mapper.enable(SerializationFeature.INDENT_OUTPUT);
      mapper.writeValue(new File(fileName), realmRepresentation);

      System.out.println("Realm '" + realm + "' exported to '" + fileName + "'.");
    }
    catch(IOException e)
    {
      System.err.println("Failed to write realm export to '" + fileName + "': " + e.getMessage());
      log.error("Failed to write realm export to {}: {}", fileName, e.getMessage());
    }
    catch(Exception e)
    {
      System.err.println("Error exporting realm '" + realm + "': " + e.getMessage());
      log.error("Error exporting realm {}: {}", realm, e.getMessage());
    }
  }

  public void exportAuthenticationFlow(String flowName, String fileName)
  {
    log.info("Exporting authentication flow '{}' of realm '{}' to file '{}'",
      flowName, realm, fileName);

    if(flowName == null || flowName.isBlank())
    {
      System.err.println("Flow name must not be empty.");
      return;
    }

    if(fileName == null || fileName.isBlank())
    {
      System.err.println("Filename must not be empty.");
      return;
    }

    try
    {
      AuthenticationManagementResource flows = keycloak.realm(realm).flows();

      AuthenticationFlowRepresentation flow = flows.getFlows().stream()
        .filter(f -> flowName.equals(f.getAlias()))
        .findFirst()
        .orElse(null);

      if(flow == null)
      {
        System.err.println("Authentication flow '" + flowName + "' not found in realm '" + realm + "'.");
        return;
      }

      List<AuthenticationExecutionInfoRepresentation> executions =
        flows.getExecutions(flow.getAlias());

      Map<String, AuthenticatorConfigRepresentation> authenticatorConfigs = new LinkedHashMap<>();
      for(AuthenticationExecutionInfoRepresentation execution : executions)
      {
        String configId = execution.getAuthenticationConfig();
        if(configId != null &&  ! configId.isBlank() &&  ! authenticatorConfigs.containsKey(configId))
        {
          try
          {
            AuthenticatorConfigRepresentation config = flows.getAuthenticatorConfig(configId);
            authenticatorConfigs.put(configId, config);
          }
          catch(Exception e)
          {
            log.warn("Could not fetch authenticator config '{}': {}", configId, e.getMessage());
          }
        }
      }

      Map<String, Object> export = new LinkedHashMap<>();
      export.put("realm", realm);
      export.put("flow", flow);
      export.put("executions", executions);
      export.put("authenticatorConfigs", authenticatorConfigs);

      ObjectMapper mapper = new ObjectMapper();
      mapper.enable(SerializationFeature.INDENT_OUTPUT);
      mapper.writeValue(new File(fileName), export);

      System.out.println("Authentication flow '" + flowName + "' exported to '" + fileName + "'.");
    }
    catch(IOException e)
    {
      System.err.println("Failed to write flow export to '" + fileName + "': " + e.getMessage());
      log.error("Failed to write flow export to {}: {}", fileName, e.getMessage());
    }
    catch(Exception e)
    {
      System.err.println("Error exporting authentication flow '" + flowName + "': " + e.getMessage());
      log.error("Error exporting authentication flow {}: {}", flowName, e.getMessage());
    }
  }

  public void importRealm(String fileName)
  {
    log.info("Importing realm from file '{}'", fileName);

    if(fileName == null || fileName.isBlank())
    {
      System.err.println("Filename must not be empty.");
      return;
    }

    File file = new File(fileName);
    if( ! file.isFile() ||  ! file.canRead())
    {
      System.err.println("File '" + fileName + "' does not exist or is not readable.");
      return;
    }

    try
    {
      ObjectMapper mapper = new ObjectMapper();
      RealmRepresentation realmRepresentation =
        mapper.readValue(file, RealmRepresentation.class);

      String targetRealm = realmRepresentation.getRealm();
      if(targetRealm == null || targetRealm.isBlank())
      {
        System.err.println("Realm representation in '" + fileName + "' has no 'realm' attribute.");
        return;
      }

      keycloak.realms().create(realmRepresentation);
      System.out.println("Realm '" + targetRealm + "' imported from '" + fileName + "'.");
    }
    catch(IOException e)
    {
      System.err.println("Failed to read realm import from '" + fileName + "': " + e.getMessage());
      log.error("Failed to read realm import from {}: {}", fileName, e.getMessage());
    }
    catch(Exception e)
    {
      System.err.println("Error importing realm from '" + fileName + "': " + e.getMessage());
      log.error("Error importing realm from {}: {}", fileName, e.getMessage());
    }
  }

  public void importAuthenticationFlow(String fileName)
  {
    log.info("Importing authentication flow from file '{}' into realm '{}'",
      fileName, realm);

    if(fileName == null || fileName.isBlank())
    {
      System.err.println("Filename must not be empty.");
      return;
    }

    File file = new File(fileName);
    if( ! file.isFile() ||  ! file.canRead())
    {
      System.err.println("File '" + fileName + "' does not exist or is not readable.");
      return;
    }

    try
    {
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> data = mapper.readValue(file, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});

      Object flowNode = data.get("flow");
      if(flowNode == null)
      {
        System.err.println("File '" + fileName + "' contains no 'flow' attribute.");
        return;
      }

      AuthenticationFlowRepresentation flow = mapper.convertValue(
        flowNode, AuthenticationFlowRepresentation.class);

      flow.setId(null);

      AuthenticationManagementResource flows = keycloak.realm(realm).flows();

      boolean exists = flows.getFlows().stream()
        .anyMatch(f -> flow.getAlias().equals(f.getAlias()));
      if(exists)
      {
        System.err.println("Authentication flow '" + flow.getAlias()
          + "' already exists in realm '" + realm + "'.");
        return;
      }

      try(Response response = flows.createFlow(flow))
      {
        handleFlowCreateResponse(response, flow.getAlias());
        if(response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL)
        {
          return;
        }
      }

      Object configsNode = data.get("authenticatorConfigs");
      if(configsNode instanceof Map<?, ?> rawConfigs &&  ! rawConfigs.isEmpty())
      {
        Map<String, AuthenticatorConfigRepresentation> configs =
          mapper.convertValue(rawConfigs,
            new com.fasterxml.jackson.core.type.TypeReference<Map<String, AuthenticatorConfigRepresentation>>() {});

        List<AuthenticationExecutionInfoRepresentation> importedExecutions =
          flows.getExecutions(flow.getAlias());

        Object executionsNode = data.get("executions");
        List<AuthenticationExecutionInfoRepresentation> originalExecutions =
          executionsNode != null
            ? mapper.convertValue(executionsNode,
                new com.fasterxml.jackson.core.type.TypeReference<List<AuthenticationExecutionInfoRepresentation>>() {})
            : List.of();

        for(int i = 0; i < importedExecutions.size() && i < originalExecutions.size(); i++)
        {
          AuthenticationExecutionInfoRepresentation imported = importedExecutions.get(i);
          AuthenticationExecutionInfoRepresentation original = originalExecutions.get(i);
          String originalConfigId = original.getAuthenticationConfig();
          if(originalConfigId != null && imported.getAuthenticationConfig() == null
            && configs.containsKey(originalConfigId))
          {
            AuthenticatorConfigRepresentation cfg = configs.get(originalConfigId);
            AuthenticatorConfigRepresentation cleanCfg = new AuthenticatorConfigRepresentation();
            cleanCfg.setAlias(cfg.getAlias());
            cleanCfg.setConfig(cfg.getConfig());
            try(Response cfgResponse = flows.newExecutionConfig(imported.getId(), cleanCfg))
            {
              if(cfgResponse.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL)
              {
                log.warn("Failed to attach authenticator config '{}' to execution '{}': HTTP {}",
                  cfg.getAlias(), imported.getId(), cfgResponse.getStatus());
              }
            }
          }
        }
      }
    }
    catch(IOException e)
    {
      System.err.println("Failed to read flow import from '" + fileName + "': " + e.getMessage());
      log.error("Failed to read flow import from {}: {}", fileName, e.getMessage());
    }
    catch(Exception e)
    {
      System.err.println("Error importing authentication flow from '" + fileName + "': " + e.getMessage());
      log.error("Error importing authentication flow from {}: {}", fileName, e.getMessage());
    }
  }

  private void handleFlowCreateResponse(Response response, String alias)
  {
    if(response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL)
    {
      System.out.println("Authentication flow '" + alias
        + "' imported into realm '" + realm + "'.");
    }
    else
    {
      String error = response.readEntity(String.class);
      System.err.println("Failed to import authentication flow '" + alias
        + "': HTTP " + response.getStatus() + " — " + error);
    }
  }

  public void showClient(String clientName)
  {
    log.info("Showing details for client: {}", clientName);
    List<ClientRepresentation> clients = keycloak.realm(realm).clients().findByClientId(clientName);

    if(clients.isEmpty())
    {
      System.out.println("Client with name '" + clientName + "' not found.");
      return;
    }

    ClientRepresentation client = clients.get(0);
    String id = client.getId();

    System.out.println("\nClient Details for: " + clientName);
    System.out.println("-----------------------------------");
    System.out.println("ID          : " + id);
    System.out.println("Client ID   : " + client.getClientId());
    System.out.println("Name        : " + client.getName());
    System.out.println("Description : " + client.getDescription());
    System.out.println("Enabled     : " + client.isEnabled());
    System.out.println("Root URL    : " + client.getRootUrl());
    System.out.println("Base URL    : " + client.getBaseUrl());

    // Scopes
    System.out.println("\nDefault Client Scopes:");
    keycloak.realm(realm).clients().get(id).getDefaultClientScopes().forEach(scope ->
    {
      System.out.println("  - " + scope.getName());
    });

    System.out.println("\nOptional Client Scopes:");
    keycloak.realm(realm).clients().get(id).getOptionalClientScopes().forEach(scope ->
    {
      System.out.println("  - " + scope.getName());
    });

    // Roles
    List<RoleRepresentation> clientRoles = keycloak.realm(realm).clients().get(id).roles().list();
    if( ! clientRoles.isEmpty())
    {
      System.out.println("\nClient Roles:");
      clientRoles.stream()
        .sorted((r1, r2) -> r1.getName().compareToIgnoreCase(r2.getName()))
        .forEach(role ->
        {
          System.out.println("  - " + role.getName());
        });
    }
    System.out.println();
  }

}
