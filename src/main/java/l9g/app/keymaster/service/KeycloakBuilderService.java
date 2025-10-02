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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Service
@Slf4j
@Getter
public class KeycloakBuilderService
{
  private final String realm;

  private final Keycloak keycloak;

  public KeycloakBuilderService( 
    @Value("${keycloak.realm}") String realm,
    @Value("${keycloak.client-id}") String clientId,
    @Value("${keycloak.client-secret}") String clientSecret,
    @Value("${keycloak.base-url}") String baseUrl
  )
  {
    log.debug("initialize");
    this.realm = realm;
    
    this.keycloak = KeycloakBuilder.builder()
      .serverUrl(baseUrl)
      .realm(realm)
      .clientId(clientId)
      .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
      .clientSecret(clientSecret)
      .build();
  }
  
}
