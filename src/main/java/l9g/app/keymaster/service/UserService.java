/*
 * Copyright 2025 Thorsten Ludewig (t.ludewig@gmail.com).
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

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Service
@Slf4j
public class UserService
{
  private final static int PAGE_SIZE = 100;

  private final String realm;

  private final Keycloak keycloak;

  private final List<UserRepresentation> users = new ArrayList<>();

  public UserService(KeycloakBuilderService keycloakConnection)
  {
    realm = keycloakConnection.getRealm();
    keycloak = keycloakConnection.getKeycloak();
  }

  public List<UserRepresentation> users(boolean reload)
  {
    log.debug("users({})", reload);

    if(reload || users.isEmpty())
    {
      users.clear();
      int numberOfUsers = keycloak.realm(realm).users().count();
      log.debug("numberOfUsers={}", numberOfUsers);
      int index = 0;
      List<UserRepresentation> usersPage;
      do
      {
        log.debug("index={}", index);
        usersPage = keycloak.realm(realm).users().list(index, (numberOfUsers
          - index >= PAGE_SIZE) ? PAGE_SIZE : numberOfUsers - index);
        log.debug("index={} done", index);
        users.addAll(usersPage);
        index += PAGE_SIZE;
        log.debug("load users {}", index);
      }
      while(usersPage.size() == PAGE_SIZE && index < numberOfUsers);
    }

    return users;
  }

  public List<UserRepresentation> users()
  {
    return users(false);
  }

}
