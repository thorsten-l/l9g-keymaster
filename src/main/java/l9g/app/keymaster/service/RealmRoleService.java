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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.stereotype.Service;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Service
@Slf4j
public class RealmRoleService
{
  private final String realm;

  private final Keycloak keycloak;

  private final List<RoleRepresentation> realmRolesList = new ArrayList<>();

  private final Map<String, Set<RoleRepresentation>> roleCompositesMap = new HashMap<>();

  private final Map<String, RoleResource> roleResourcesMap = new HashMap<>();

  public RealmRoleService(KeycloakBuilderService keycloakConnection)
  {
    realm = keycloakConnection.getRealm();
    keycloak = keycloakConnection.getKeycloak();
  }

  public synchronized List<RoleRepresentation> roles(boolean reload)
  {
    if(reload || realmRolesList.isEmpty())
    {
      log.debug("reload realm roles");
      realmRolesList.clear();
      realmRolesList.addAll(keycloak.realm(realm).roles().list());
    }
    else
    {
      log.debug("realm roles from cache");
    }

    return realmRolesList;
  }

  public List<RoleRepresentation> roles()
  {
    return RealmRoleService.this.roles(false);
  }

  public synchronized RoleResource resourceByName(String roleName)
  {
    RoleResource resource = roleResourcesMap.get(roleName);

    if(resource == null)
    {
      resource = keycloak.realm(realm).roles().get(roleName);
      roleResourcesMap.put(roleName, resource);
    }

    return resource;
  }

  public synchronized Set<RoleRepresentation> compositesByName(String roleName)
  {
    Set<RoleRepresentation> composites = roleCompositesMap.get(roleName);

    if(composites == null)
    {
      RoleResource roleResource = resourceByName(roleName);
      composites = roleResource.getRoleComposites();
      roleCompositesMap.put(roleName, composites);
    }

    return composites;
  }

}
