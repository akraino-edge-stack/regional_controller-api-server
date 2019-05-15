/*
 * Copyright (c) 2019 AT&T Intellectual Property. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.akraino.regional_controller.db;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.akraino.regional_controller.beans.User;

/**
 * The StandardDBWithLDAP uses an LDAP server for users, and an SQL DB for the other items required.
 * This is the "production" DB implementation.
 */
@SuppressWarnings("unused")
public class StandardDBWithLDAP extends StandardDB {
	private static final String LDAP_CONTEXT = "com.sun.jndi.ldap.LdapCtxFactory";

	private final String ldap_url;
	private final String ldap_user;
	private final String ldap_password;
	private final String search_base;

	public StandardDBWithLDAP(Properties api_props) throws ClassNotFoundException {
		super(api_props);
		this.ldap_url      = api_props.getProperty("ldap.url");
		this.ldap_user     = api_props.getProperty("ldap.user");
		this.ldap_password = api_props.getProperty("ldap.password");
		this.search_base   = api_props.getProperty("ldap.search_base");
	}

	@Override
	public User getUser(String name) {
		return getUserByFilter("(cn="+name+")");
	}

	@Override
	protected User getUserByUuid(String uuid) {
		return getUserByFilter("(uid="+uuid+")");
	}

	private User getUserByFilter(String filter) {
		Hashtable<String, String> env = new Hashtable<>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, LDAP_CONTEXT);
		env.put(Context.PROVIDER_URL,            ldap_url);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL,      ldap_user);
		env.put(Context.SECURITY_CREDENTIALS,    ldap_password);

		SearchControls sc = new SearchControls();
		sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
		sc.setTimeLimit(30000);
		sc.setCountLimit(1000);

		LdapContext ctx = null;
		try {
			ctx = new InitialLdapContext(env, null);
			NamingEnumeration<?> results = ctx.search(search_base, filter, sc);
			while (results.hasMore()) {
			  SearchResult sr = (SearchResult) results.next();
			  Attributes attrs = sr.getAttributes();
			  String uuid     = safeget(attrs, "uid");
			  String name     = safeget(attrs, "cn");
			  String password = safeget(attrs, "userPassword");	// hash this!
			  String description = safeget(attrs, "description");
			  System.out.println(uuid + " / " + name + " / " + password + " / " + description);
			  User u = new User(uuid, name, password, (description == null) ? "" : description);
			  return u;
			}
			results.close();
		} catch (NamingException e) {
			logger.warn(e);
		} finally {
			if (ctx != null) {
				try {
					ctx.close();
				} catch (NamingException e) {
					// ignore
				}
			}
		}
		return null;
	}
	private String safeget(Attributes attrs, String key) {
		Attribute a = attrs.get(key);
		if (a != null) {
			try {
				Object o = a.get();
				if (o != null) {
					return o.toString();
				}
			} catch (NamingException e) {
			}
		}
		return null;
	}

	/*
	 * uuid -> uid
	 * name -> cn
	 * description -> displayName (or description)
	 * pwhash -> ??
	 */
	public static void main(String[] a) throws NamingException, ClassNotFoundException {
		Properties p = new Properties();
		p.setProperty("ldap.url",         "ldap://k18:389/");
		p.setProperty("ldap.user",        "cn=admin,dc=akraino,dc=demo");
		p.setProperty("ldap.password",    "abc123");
		p.setProperty("ldap.search_base", "dc=akraino,dc=demo");
		p.setProperty("db.driver",        "com.mysql.jdbc.Driver");

		StandardDBWithLDAP xx = new StandardDBWithLDAP(p);
		User u =
//			xx.getUserByUuid("2d3a342e-6374-11e9-8b05-8333548995aa");
			xx.getUser("Noaccess User");
		System.out.println(u);
	}
}