/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.testsuite.console.clients;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;

import static org.junit.Assert.*;
import org.junit.Before;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testsuite.console.page.clients.mappers.ClientMapper;
import org.keycloak.testsuite.console.page.clients.mappers.ClientMappers;
import org.keycloak.testsuite.console.page.clients.mappers.CreateClientMappers;
import static org.keycloak.testsuite.console.page.clients.mappers.CreateClientMappersForm.*;

/**
 * 
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 * 
 * TODO: saml mappers
 */
public class ClientMappersTest extends AbstractClientTest {

    private String id;
    
    @Page
    private ClientMappers clientMappersPage;
    @Page
    private ClientMapper clientMapperPage;

    @Page 
    private CreateClientMappers createClientMappersPage;
    
    @Before
    public void beforeClientMappersTest() {
        ClientRepresentation newClient = createClientRepresentation(TEST_CLIENT_ID, TEST_REDIRECT_URIS);
        testRealmResource().clients().create(newClient).close();
        
        id = findClientByClientId(TEST_CLIENT_ID).getId();
        clientMappersPage.setId(id);
        clientMappersPage.navigateTo();
    }
    
    private ProtocolMapperRepresentation findClientMapperByName(String mapperName) {
        ProtocolMapperRepresentation found = null;
        for (ProtocolMapperRepresentation mapper : testRealmResource().clients().get(id).getProtocolMappers().getMappers()) {
            if (mapperName.equals(mapper.getName())) {
                found = mapper;
            }
        }
        return found;
    }
    
    private void setInitialValues(String name, boolean consentRequired, String consentText) {
        createClientMappersPage.form().setName(name);
        createClientMappersPage.form().setConsentRequired(consentRequired);
        if (consentRequired) {
            createClientMappersPage.form().setConsentText(consentText);
        }
    }
    
    @Test
    public void testOIDCHardcodedRole() {
        //create
        clientMappersPage.mapperTable().createMapper();
        setInitialValues("hardcoded role", true, "Consent Text");
        createClientMappersPage.form().setMapperType(HARDCODED_ROLE);
        createClientMappersPage.form().selectRole(REALM_ROLE, "offline_access", null);
        createClientMappersPage.form().save();
        assertFlashMessageSuccess();
        
        //check
        ProtocolMapperRepresentation found = findClientMapperByName("hardcoded role");
        assertNotNull(found);
        
        assertTrue(found.isConsentRequired());
        assertEquals("Consent Text", found.getConsentText());
        assertEquals("oidc-hardcoded-role-mapper", found.getProtocolMapper());
        Map<String, String> config = found.getConfig();
        
        assertEquals(1, config.size());
        assertEquals("offline_access", config.get("role"));
        
        //edit
        createClientMappersPage.form().selectRole(CLIENT_ROLE, "view-profile", "account");
        createClientMappersPage.form().save();
        assertFlashMessageSuccess();
        
        //check
        config = findClientMapperByName("hardcoded role").getConfig();
        assertEquals("account.view-profile", config.get("role"));
        
        //delete
        clientMapperPage.setMapperId(found.getId());
        clientMapperPage.delete();
        assertFlashMessageSuccess();
        
        //check
        assertNull(findClientMapperByName("hardcoded role"));
    }
    
    @Test
    public void testOIDCHardcodedClaim() {
        //create
        clientMappersPage.mapperTable().createMapper();
        setInitialValues("hardcoded claim", false, null);
        createClientMappersPage.form().setMapperType(HARDCODED_CLAIM);
        createClientMappersPage.form().setTokenClaimName("claim name");
        createClientMappersPage.form().setTokenClaimValue("claim value");
        createClientMappersPage.form().setClaimJSONType("long");
        createClientMappersPage.form().setAddToIDToken(true);
        createClientMappersPage.form().setAddToAccessToken(true);
        createClientMappersPage.form().save();
        assertFlashMessageSuccess();
        
        //check
        ProtocolMapperRepresentation found = findClientMapperByName("hardcoded claim");
        assertNotNull(found);
        
        assertFalse(found.isConsentRequired());
        assertEquals("oidc-hardcoded-claim-mapper", found.getProtocolMapper());
        
        Map<String, String> config = found.getConfig();
        assertEquals("true", config.get("id.token.claim"));
        assertEquals("true", config.get("access.token.claim"));
        assertEquals("claim name", config.get("claim.name"));
        assertEquals("claim value", config.get("claim.value"));
        assertEquals("long", config.get("jsonType.label"));
    }
    
    @Test
    public void testOIDCUserSessionNote() {
        //create
        clientMappersPage.mapperTable().createMapper();
        setInitialValues("user session note", false, null);
        createClientMappersPage.form().setMapperType(USER_SESSION_NOTE);
        createClientMappersPage.form().setUserSessionNote("session note");
        createClientMappersPage.form().setTokenClaimName("claim name");
        createClientMappersPage.form().setClaimJSONType("int");
        createClientMappersPage.form().setAddToIDToken(false);
        createClientMappersPage.form().setAddToAccessToken(false);
        createClientMappersPage.form().save();
        assertFlashMessageSuccess();
        
        //check
        ProtocolMapperRepresentation found = findClientMapperByName("user session note");
        assertNotNull(found);
        
        assertFalse(found.isConsentRequired());
        assertEquals("oidc-usersessionmodel-note-mapper", found.getProtocolMapper());
        
        Map<String, String> config = found.getConfig();
        assertNull(config.get("id.token.claim"));
        assertNull(config.get("access.token.claim"));
        assertEquals("claim name", config.get("claim.name"));
        assertEquals("session note", config.get("user.session.note"));
        assertEquals("int", config.get("jsonType.label"));
    }

    @Test
    public void testOIDCRoleName() {
        //create
        clientMappersPage.mapperTable().createMapper();
        setInitialValues("role name", false, null);
        createClientMappersPage.form().setMapperType(ROLE_NAME_MAPPER);
        createClientMappersPage.form().setRole("offline_access");
        createClientMappersPage.form().setNewRole("new role");
        createClientMappersPage.form().save();
        assertFlashMessageSuccess();
        
        //check
        ProtocolMapperRepresentation found = findClientMapperByName("role name");
        assertEquals("oidc-role-name-mapper", found.getProtocolMapper());
        
        Map<String, String> config = found.getConfig();
        assertEquals("offline_access", config.get("role"));
        assertEquals("new role", config.get("new.role.name"));
    }

    @Test
    public void testOIDCUserAddress() {
        //create
        clientMappersPage.mapperTable().createMapper();
        setInitialValues("user address", false, null);
        createClientMappersPage.form().setMapperType(USERS_FULL_NAME);
        createClientMappersPage.form().save();
        assertFlashMessageSuccess();
        
        //check
        ProtocolMapperRepresentation found = findClientMapperByName("user address");
        assertEquals("oidc-full-name-mapper", found.getProtocolMapper());
    }
    
    @Test
    public void testOIDCUserFullName() {
        //create
        clientMappersPage.mapperTable().createMapper();
        setInitialValues("user full name", false, null);
        createClientMappersPage.form().setMapperType(USERS_FULL_NAME);
        createClientMappersPage.form().save();
        assertFlashMessageSuccess();
        
        //check
        ProtocolMapperRepresentation found = findClientMapperByName("user full name");
        assertEquals("oidc-full-name-mapper", found.getProtocolMapper());
    }
    
    @Test
    public void testOIDCUserAttribute() {
        //create
        clientMappersPage.mapperTable().createMapper();
        setInitialValues("user attribute", false, null);
        createClientMappersPage.form().setMapperType(USER_ATTRIBUTE);
        createClientMappersPage.form().setUserAttribute("user attribute");
        createClientMappersPage.form().setMultivalued(true);
        createClientMappersPage.form().save();
        assertFlashMessageSuccess();
        
        //check
        ProtocolMapperRepresentation found = findClientMapperByName("user attribute");
        assertEquals("oidc-usermodel-attribute-mapper", found.getProtocolMapper());
        
        Map<String, String> config = found.getConfig();
        assertEquals("true", config.get("multivalued"));
        assertEquals("user attribute", config.get("user.attribute"));
    }

    @Test
    public void testOIDCUserProperty() {
        //create
        clientMappersPage.mapperTable().createMapper();
        setInitialValues("user property", false, null);
        createClientMappersPage.form().setMapperType(USER_PROPERTY);
        createClientMappersPage.form().setProperty("property");
        createClientMappersPage.form().save();
        assertFlashMessageSuccess();
        
        //check
        ProtocolMapperRepresentation found = findClientMapperByName("user property");
        assertEquals("oidc-usermodel-property-mapper", found.getProtocolMapper());
        
        Map<String, String> config = found.getConfig();
        assertEquals("property", config.get("user.attribute"));
    }
    
    @Test
    public void testOIDCGroupMembership() {
        //create
        clientMappersPage.mapperTable().createMapper();
        setInitialValues("group membership", false, null);
        createClientMappersPage.form().setMapperType(GROUP_MEMBERSHIP);
        createClientMappersPage.form().setFullGroupPath(true);
        createClientMappersPage.form().save();
        assertFlashMessageSuccess();
        
        //check
        ProtocolMapperRepresentation found = findClientMapperByName("group membership");
        assertEquals("oidc-group-membership-mapper", found.getProtocolMapper());
        
        Map<String, String> config = found.getConfig();
        assertEquals("true", config.get("full.path"));
    }
    
    @Test
    public void testOIDCEditMapper() {
        //prepare data
        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName("mapper name");
        mapper.setConsentRequired(true);
        mapper.setConsentText("consent text");
        mapper.setProtocol("openid-connect");
        mapper.setProtocolMapper("oidc-usersessionmodel-note-mapper");
        
        Map<String, String> config = new HashMap<>();
        config.put("access.token.claim", "true");
        config.put("id.token.claim", "true");
        config.put("claim.name", "claim name");
        config.put("jsonType.label", "String");
        config.put("user.session.note", "session note");
        
        mapper.setConfig(config);
        
        //insert data
        testRealmResource().clients().get(id).getProtocolMappers().createMapper(mapper).close();
        
        //check form
        clientMapperPage.setId(id);
        String mapperId = findClientMapperByName("mapper name").getId();
        clientMapperPage.setMapperId(mapperId);
        clientMapperPage.navigateTo();
        
        assertEquals("openid-connect", clientMapperPage.form().getProtocol());
        assertEquals(mapperId, clientMapperPage.form().getMapperId());
        assertEquals("mapper name", clientMapperPage.form().getName());
        assertTrue(clientMapperPage.form().isConsentRequired());
        assertEquals("consent text", clientMapperPage.form().getConsentText());
        assertEquals("User Session Note", clientMapperPage.form().getMapperType());
        assertEquals("session note", clientMapperPage.form().getUserSessionNote());
        assertEquals("claim name", clientMapperPage.form().getTokenClaimName());
        assertEquals("String", clientMapperPage.form().getClaimJSONType());
        assertTrue(clientMapperPage.form().isAddToIDToken());
        assertTrue(clientMapperPage.form().isAddToAccessToken());
        
        //edit
        clientMapperPage.form().setConsentRequired(false);
        clientMapperPage.form().save();
        assertFlashMessageSuccess();
        
        //check
        assertFalse(findClientMapperByName("mapper name").isConsentRequired());
    }
    
    @Test
    public void testAddBuiltin() {
        clientMappersPage.mapperTable().addBuiltin();
        clientMappersPage.mapperTable().checkBuiltinMapper("locale");
        clientMappersPage.mapperTable().clickAddSelectedBuiltinMapper();
        assertFlashMessageSuccess();
        
        assertTrue("Builtin mapper \"locale\" should be present.", isMapperPresent("locale"));
        
        clientMappersPage.mapperTable().deleteMapper("locale");
        modalDialog.confirmDeletion();
        assertFlashMessageSuccess();
        
        assertFalse("Builtin mapper \"locale\" should not be present.", isMapperPresent("locale"));
    }
    
    private boolean isMapperPresent(String name) {
        List<ProtocolMapperRepresentation> mappers = testRealmResource().clients().get(id).getProtocolMappers().getMappers();
        boolean found = false;
        for (ProtocolMapperRepresentation mapper : mappers) {
            if (name.equals(mapper.getName())) {
                found = true;
            }
        }
        return found;
    }
    
    @Test
    public void testCreateMapperInvalidValues() {
        //empty mapper type
        clientMappersPage.mapperTable().createMapper();
        createClientMappersPage.form().save();
        assertFlashMessageDanger();
        
        //empty name
        createClientMappersPage.form().setMapperType(HARDCODED_ROLE);
        createClientMappersPage.form().save();
        assertFlashMessageDanger();
        
        createClientMappersPage.form().setName("");
        createClientMappersPage.form().save();
        assertFlashMessageDanger();
        
        createClientMappersPage.form().setName("name");
        createClientMappersPage.form().setName("");
        createClientMappersPage.form().save();
        assertFlashMessageDanger();
        
        //existing name
        createClientMappersPage.form().setName("email");
        createClientMappersPage.form().save();
        assertFlashMessageDanger();
    }
}
