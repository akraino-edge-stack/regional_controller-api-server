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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.akraino.regional_controller.beans.User;

/**
 * The StandardDBWithLDAP uses an LDAP server for users, and an SQL DB for the other items required.
 * This is the "production" DB implementation.
 *
 * This implementation stores ONLY the Users in LDAP.  All other tables that are required by the full
 * design which are linked to the users (such as USER_ROLES, ROLES, and ROLE_ATTRIBUTES) are still
 * stored in the underlying SQL database (indexed via UUID).  The intent is to allow an already
 * administered LDAP DB to be used for user authentication, without having to make major modifications
 * to its schema.
 *
 * This implementation makes the following assumptions:
 * 1. The user's login name is stored as the "cn" attribute in LDAP.
 * 2. The user's UUID is stored as the "uid" attribute in LDAP.
 * 3. User passwords are stored in the clear (base64 encoded) in LDAP.  The hash of the password is
 *    computed in the API code.
 */
@SuppressWarnings("unused")
public class StandardDBWithLDAP extends StandardDB {
	private static final String LDAP_CONTEXT = "com.sun.jndi.ldap.LdapCtxFactory";
	private static final String LDAP_AUTH    = "simple";

	private static final String LDAP_ORGUNIT     = "ou";
	private static final String LDAP_COMMONNAME  = "cn";
	private static final String LDAP_SURNAME     = "sn";
	private static final String LDAP_DESCRIPTION = "description";
	private static final String LDAP_USERPASSWD  = "userPassword";
	private static final String LDAP_USERID      = "uid";

	private final String ldap_url;
	private final String ldap_user;
	private final String ldap_password;
	private final String search_base;
	private final Hashtable<String, String> env;

	public StandardDBWithLDAP(Properties api_props) throws ClassNotFoundException {
		super(api_props);
		this.ldap_url      = api_props.getProperty("ldap.url");
		this.ldap_user     = api_props.getProperty("ldap.user");
		this.ldap_password = api_props.getProperty("ldap.password");
		this.search_base   = api_props.getProperty("ldap.search_base");
		this.env           = new Hashtable<>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, LDAP_CONTEXT);
		env.put(Context.SECURITY_AUTHENTICATION, LDAP_AUTH);
		env.put(Context.PROVIDER_URL,            ldap_url);
		env.put(Context.SECURITY_PRINCIPAL,      ldap_user);
		env.put(Context.SECURITY_CREDENTIALS,    ldap_password);
	}

	@Override
	public void createUser(final User u) throws SQLException {
		// Since we do not have the password (only the PW hash) we cannot create a user in LDAP.
		// If this is absolutely required by some future user of this code, then the code will
		// need to be revised in order to pass the password down the chain to this method.
		throw new SQLException("Cannot create a user via this method in the LDAP database.");

//		LdapContext ctx = null;
//		try {
//			ctx = new InitialLdapContext(env, null);
//
//			Attribute oc = new BasicAttribute("objectClass");
//			oc.add("person");
//			oc.add("inetOrgPerson");
//
//			Attributes entry = new BasicAttributes();
//			entry.put(oc);
//			entry.put(new BasicAttribute(LDAP_ORGUNIT,    "people"));
//			entry.put(new BasicAttribute(LDAP_COMMONNAME,  u.getName()));
//			entry.put(new BasicAttribute(LDAP_SURNAME,     u.getName()));
//			entry.put(new BasicAttribute(LDAP_DESCRIPTION, u.getDescription()));
//			entry.put(new BasicAttribute(LDAP_PWHASH,      u.getPasswordHash()));
//			entry.put(new BasicAttribute(LDAP_USERID,      u.getUuid()));
//
//			String entryDN = buildDN(u);
//			ctx.createSubcontext(entryDN, entry);
//
//			// Call the parent class in order to store the roles in the SQL DB
//			super.createUser(u);
//		} catch (NamingException e) {
//			logger.warn(e);
//		} finally {
//			if (ctx != null) {
//				try {
//					ctx.close();
//				} catch (NamingException e) {
//					// ignore
//				}
//			}
//		}
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
				return buildUserFromSearchResult(sr);
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

	@Override
	public List<User> getUsers() {
		List<User> users = new ArrayList<>();

		SearchControls sc = new SearchControls();
		sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
		sc.setTimeLimit(30000);
		sc.setCountLimit(1000);

		LdapContext ctx = null;
		try {
			ctx = new InitialLdapContext(env, null);
			NamingEnumeration<?> results = ctx.search(search_base, "(uid=*)", sc);
			while (results.hasMore()) {
				SearchResult sr = (SearchResult) results.next();
				User u = buildUserFromSearchResult(sr);
				users.add(u);
			}
			results.close();
			return users;
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

	@Override
	public void updateUser(final User u) throws SQLException {
		LdapContext ctx = null;
		try {
			String entryDN = buildDN(u);
			ctx = new InitialLdapContext(env, null);
			ModificationItem[] mods = new ModificationItem[] {
				new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(LDAP_DESCRIPTION, u.getDescription()))
				// add password later??
			};
			ctx.modifyAttributes(entryDN, mods);
			super.updateUser(u);	// Update roles in SQL DB
		} catch (Exception ex) {
			logger.warn(ex);
		} finally {
			if (ctx != null) {
				try {
					ctx.close();
				} catch (NamingException e) {
					// ignore
				}
			}
		}
	}

	@Override
	public void deleteUser(final User u) throws SQLException {
		LdapContext ctx = null;
		try {
			String entryDN = buildDN(u);
			ctx = new InitialLdapContext(env, null);
			ctx.destroySubcontext(entryDN);
			super.deleteUser(u);	// Delete roles in SQL DB
		} catch (Exception ex) {
			logger.warn(ex);
		} finally {
			if (ctx != null) {
				try {
					ctx.close();
				} catch (NamingException e) {
					// ignore
				}
			}
		}
	}

	private User buildUserFromSearchResult(SearchResult sr) {
		Attributes attrs   = sr.getAttributes();
		String uuid        = safeget(attrs, LDAP_USERID);
		String name        = safeget(attrs, LDAP_COMMONNAME);
		String description = safeget(attrs, LDAP_DESCRIPTION);
		String pswd        = safeget(attrs, LDAP_USERPASSWD);

		User u = new User(uuid, name, "", (description == null) ? "" : description);
		u.setPassword(pswd);
		u.setRoles(getRolesForUser(uuid));
		return u;
	}

	private String safeget(Attributes attrs, String key) {
		Attribute a = attrs.get(key);
		if (a != null) {
			try {
				Object o = a.get();
				if (o != null) {
					if (o instanceof byte[]) {
						return new String((byte[]) o);
					} else {
						return o.toString();
					}
				}
			} catch (NamingException e) {
				logger.warn(e);
			}
		}
		return null;
	}

	private String buildDN(User u) {
		return String.format("uid=%s,ou=people,%s", u.getUuid(), search_base);
	}
}
