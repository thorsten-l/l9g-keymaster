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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientRepresentation;
import org.springframework.stereotype.Service;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Service
@Slf4j
public class ClientService
{
  private final String realm;

  private final Keycloak keycloak;

  private final Map<String, ClientRepresentation> clientRepresentationMap = new HashMap<>();

  private boolean allCollected;

  public ClientService(KeycloakBuilderService keycloakConnection)
  {
    realm = keycloakConnection.getRealm();
    keycloak = keycloakConnection.getKeycloak();
  }

  public synchronized Collection<ClientRepresentation> clients(boolean reload)
  {
    if(reload ||  ! allCollected)
    {
      clientRepresentationMap.clear();
      keycloak.realm(realm).clients().findAll()
        .forEach(client -> clientRepresentationMap.put(client.getId(), client));
      if( ! clientRepresentationMap.isEmpty())
      {
        allCollected = true;
      }
    }

    return clientRepresentationMap.values();
  }

  public Collection<ClientRepresentation> clients()
  {
    return clients(false);
  }

  public synchronized ClientRepresentation clientById(String clientId)
  {
    ClientRepresentation clientRepresentation = clientRepresentationMap.get(clientId);

    if(clientRepresentation == null)
    {
      clientRepresentation = keycloak.realm(realm).clients().get(clientId).toRepresentation();
      clientRepresentationMap.put(clientId, clientRepresentation);
    }

    return clientRepresentation;
  }

}
