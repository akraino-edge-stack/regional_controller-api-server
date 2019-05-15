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

package org.akraino.regional_controller.utils;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;

import io.jsonwebtoken.Jwts;

//import com.auth0.jwt.algorithms.Algorithm;
//import com.auth0.jwt.exceptions.JWTCreationException;

/**
 * A class to handle Java Web Tokens (JWT). RFC 7519
 *
 * Ref:
 * https://github.com/jwtk/jjwt
 * https://www.baeldung.com/java-json-web-tokens-jjwt
 * https://github.com/auth0/java-jwt
 */
public class JWT {

	public static void main(String[] a) throws NoSuchAlgorithmException, InvalidKeySpecException {

//		KeySpec keyspec = new X509EncodedKeySpec(bobEncodedPubKey);
//		BigInteger b1 = new BigInteger("69").multiply(new BigInteger("69"));
//		KeySpec keyspec = new RSAPrivateKeySpec( b1, new BigInteger("68"));
//		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
//		Key key = keyFactory.generatePrivate(keyspec);

		Date now   = new Date(System.currentTimeMillis());
		Date later = new Date(System.currentTimeMillis() + (3600000L));
		String jws = Jwts
			.builder()
			.setIssuer("Akraino")
			.setSubject("login")
			.claim("name", "eby")
			.claim("IP_addr", "192.168.1.177")
			.setNotBefore( now )
			.setIssuedAt(  now )
			.setExpiration( later )
//			.signWith(key)
			.compact();
		System.out.println(now);

		int ix = jws.indexOf(".");
		if (ix > 0) {
			System.out.println(jws.substring(0, ix));
			System.out.println(jws.substring(ix+1));
		} else {
			System.out.println(jws);
		}
	}
}
