/******************************************************************************* 
 * Copyright (c) 2007 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package com.openshift.client.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fest.assertions.AssertExtension;

import com.openshift.client.IApplication;
import com.openshift.client.ICartridge;
import com.openshift.client.ICartridgeConstraint;
import com.openshift.client.IDomain;
import com.openshift.client.IEmbeddableCartridge;
import com.openshift.client.IEmbeddedCartridge;
import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.IUser;
import com.openshift.client.OpenShiftException;

/**
 * @author André Dietisheim
 */
public class ApplicationAssert implements AssertExtension {

	public static final Pattern APPLICATION_URL_PATTERN = Pattern.compile("https*://(.+)-([^\\.]+)\\.(.+)/(.*)");
	public static final Pattern GIT_URL_PATTERN = Pattern.compile("ssh://(.+)@(.+)-([^\\.]+)\\.(.+)/~/git/(.+).git/");
	
	private IApplication application;

	public ApplicationAssert(IApplication application) {
		this.application = application;
	}

	public ApplicationAssert hasName(String name) {
		assertEquals(name, application.getName());
		return this;
	}

	public ApplicationAssert hasUUID(String uuid) {
		assertEquals(uuid, application.getUUID());
		return this;
	}

	public ApplicationAssert hasUUID() {
		assertNotNull(application.getUUID());
		return this;
	}

	public ApplicationAssert hasCartridge(ICartridge cartridge) {
		assertEquals(cartridge, application.getCartridge());
		return this;
	}

	public ApplicationAssert hasCreationTime(String creationTime) {
		assertEquals(creationTime, application.getCreationTime());
		return this;
	}

	public ApplicationAssert hasCreationTime() {
		assertNotNull(application.getCreationTime());
		return this;
	}

	public ApplicationAssert hasGitUrl(String gitUrl) {
		assertEquals(gitUrl, application.getGitUrl());
		return this;
	}

	public ApplicationAssert hasValidGitUrl() {
		Matcher matcher = GIT_URL_PATTERN.matcher(application.getGitUrl());
		assertTrue(matcher.matches());
		assertEquals(5, matcher.groupCount());
		
		assertEquals(application.getUUID(), matcher.group(1));
		assertEquals(application.getName(), matcher.group(2));
		assertEquals(application.getDomain().getSuffix(), matcher.group(4));
		assertEquals(application.getName(), matcher.group(5));

		return this;
	}

	public ApplicationAssert hasApplicationUrl(String applicationUrl) {
		assertEquals(applicationUrl, application.getApplicationUrl());
		return this;
	}

	public ApplicationAssert hasValidApplicationUrl() {
		assertApplicationUrl();
		return this;
	}

	private void assertApplicationUrl() {
		Matcher matcher = APPLICATION_URL_PATTERN.matcher(application.getApplicationUrl());
		assertTrue(matcher.matches());
		assertTrue(matcher.groupCount() >= 3);

		assertEquals(application.getName(), matcher.group(1));
		IDomain domain = application.getDomain();
		assertEquals(domain.getId(), matcher.group(2));
		assertEquals(domain.getSuffix(), matcher.group(3));
	}
	
	public ApplicationAssert hasEmbeddedCartridges(ICartridgeConstraint constraint) throws OpenShiftException {
		Collection<IEmbeddableCartridge> embeddableCartridges = constraint.getMatching(getConnection(application).getEmbeddableCartridges());
		for (IEmbeddableCartridge cartridge : constraint.getMatching(embeddableCartridges)) {
			assertTrue(application.hasEmbeddedCartridge(cartridge));
		}

		return this;
	}

	public ApplicationAssert hasEmbeddableCartridges(String... embeddableCartridgeNames) throws OpenShiftException {
		if (embeddableCartridgeNames.length == 0) {
			assertEquals(0, application.getEmbeddedCartridges().size());
		}

		for (String cartridgeName : embeddableCartridgeNames) {
			assertTrue(application.hasEmbeddedCartridge(cartridgeName));
		}

		return this;
	}

	public ApplicationAssert hasEmbeddableCartridges(int numberOf) {
		assertNotNull(application.getEmbeddedCartridges());
		assertEquals(numberOf, application.getEmbeddedCartridges().size());
		return this;
	}
	
	public ApplicationAssert hasNotEmbeddableCartridges(String... embeddableCartridgeNames) throws OpenShiftException {		
		for (String cartridgeName : embeddableCartridgeNames) {
			assertFalse(application.hasEmbeddedCartridge(cartridgeName));
		}

		return this;
	}

	public ApplicationAssert hasNotEmbeddableCartridges(ICartridgeConstraint... constraints) throws OpenShiftException {		
		for (ICartridgeConstraint constraint : constraints) {
			assertTrue(application.getEmbeddedCartridges(constraint).isEmpty());
		}

		return this;
	}


	public ApplicationAssert hasAlias(String... aliasNames) {
		if (aliasNames.length == 0) {
			assertEquals(0, application.getAliases().size());
		}

		for (String cartridgeName : aliasNames) {
			assertTrue(application.hasAlias(cartridgeName));
		}

		return this;
	}

	public void hasNotEmbeddableCartridge(IEmbeddableCartridge cartridge) {
		hasNotEmbeddableCartridge(cartridge.getName());
	}

	public void hasNotEmbeddableCartridge(String name) {
		assertNull(getEmbeddableCartridge(name));
	}

	public void assertThatDoesntContainCartridges(Collection<IEmbeddableCartridge> shouldNotBeContained, List<IEmbeddedCartridge> cartridges) {
		for(IEmbeddableCartridge shouldNot : shouldNotBeContained) {
			assertFalse(cartridges.contains(shouldNot));
		}
	}

	private IEmbeddedCartridge getEmbeddableCartridge(String name) {
		IEmbeddedCartridge matchingCartridge = null;
		for (IEmbeddedCartridge cartridge : application.getEmbeddedCartridges()) {
			if (name.equals(cartridge.getName())) {
				matchingCartridge = cartridge;
				break;
			}
		}
		return matchingCartridge;
	}

	public void assertThatContainsCartridges(Collection<IEmbeddableCartridge> shouldBeContained, List<IEmbeddedCartridge> cartridgesToCheck) {
		for (IEmbeddableCartridge cartridge : shouldBeContained) {
			assertTrue(cartridgesToCheck.contains(cartridge));
		}
	}


	private IOpenShiftConnection getConnection(IApplication application) {
		IDomain domain = application.getDomain();
		assertNotNull(domain);
		IUser user = domain.getUser();
		assertNotNull(user);
		IOpenShiftConnection connection = user.getConnection();
		assertNotNull(connection);
		return connection;
	}

}
