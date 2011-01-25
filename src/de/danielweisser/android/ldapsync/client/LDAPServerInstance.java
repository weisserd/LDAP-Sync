/*
 * Copyright 2010 Daniel Weisser
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

package de.danielweisser.android.ldapsync.client;

import java.io.Serializable;

import javax.net.SocketFactory;

import android.util.Log;

import com.unboundid.ldap.sdk.ExtendedResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;

/**
 * Encapsulates a LDAP directory server instance and provides methods to access the server. This is based on code from the UnboundID LDAP Client.
 * 
 * @author <a href="mailto:daniel.weisser@gmx.de">Daniel Weisser</a>
 */
public final class LDAPServerInstance implements Serializable {

	private static final String TAG = "LDAPServerInstance";

	/**
	 * The serial version UID for this serializable class.
	 */
	private static final long serialVersionUID = -7633400003887348205L;


	/**
	 * The encryption method (0 - no encryption, 1 - SSL, 2 - StartTLS)
	 */
	private final int encryption;

	/**
	 * The host address of the LDAP server
	 */
	private final String host;

	/**
	 * The port number of the LDAP server
	 */
	private final int port;

	/**
	 * The DN to use to bind to the server.
	 */
	private final String bindDN;

	/**
	 * The password to use to bind to the server.
	 */
	private final String bindPW;

	/**
	 * Creates a new LDAP server instance with the provided information.
	 * 
	 * @param host
	 *            The address of the server. It must not be {@code null}.
	 * @param port
	 *            The port number for the server. It must be between 1 and 65535.
	 * @param encryption
	 *            The encryption method (0 - no encryption, 1 - SSL, 2 - StartTLS)
	 * @param bindDN
	 *            The DN to use to bind to the server. It may be {@code null} or empty if no authentication should be performed.
	 * @param bindPW
	 *            The password to use to bind to the server. It may be {@code null} or empty if no authentication should be performed.
	 */
	public LDAPServerInstance(final String host, final int port, final int encryption, final String bindDN, final String bindPW) {
		this.host = host;
		this.port = port;
		this.encryption = encryption;

		this.bindDN = (bindDN == null) || (bindDN.length() == 0) ? null : bindDN;
		this.bindPW = (bindPW == null) || (bindPW.length() == 0) ? null : bindPW;
	}

	/**
	 * Retrieves an LDAP connection that may be used to communicate with the LDAP server.
	 * 
	 * @return An LDAP connection that may be used to communicate with the LDAP server.
	 * 
	 * @throws LDAPException
	 *             If a problem occurs while attempting to establish the connection.
	 */
	public LDAPConnection getConnection() throws LDAPException {
		Log.d(TAG, "Trying to connect to: " + toString());
		SocketFactory socketFactory = null;
		if (usesSSL()) {
			final SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
			try {
				socketFactory = sslUtil.createSSLSocketFactory();
			} catch (Exception e) {
				Log.e(TAG, "getConnection", e);
				throw new LDAPException(ResultCode.LOCAL_ERROR, "Cannot initialize SSL", e);
			}
		}

		final LDAPConnectionOptions options = new LDAPConnectionOptions();
		options.setAutoReconnect(true);
		options.setConnectTimeoutMillis(30000);
		options.setFollowReferrals(false);
		options.setMaxMessageSize(0);

		final LDAPConnection conn = new LDAPConnection(socketFactory, options, host, port);

		if (usesStartTLS()) {
			final SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
			try {
				final ExtendedResult r = conn.processExtendedOperation(new StartTLSExtendedRequest(sslUtil.createSSLContext()));
				if (r.getResultCode() != ResultCode.SUCCESS) {
					throw new LDAPException(r);
				}
			} catch (LDAPException le) {
				Log.e(TAG, "getConnection", le);
				conn.close();
				throw le;
			} catch (Exception e) {
				Log.e(TAG, "getConnection", e);
				conn.close();
				throw new LDAPException(ResultCode.CONNECT_ERROR, "Cannot initialize StartTLS", e);
			}
		}

		if ((bindDN != null) && (bindPW != null)) {
			try {
				conn.bind(bindDN, bindPW);
			} catch (LDAPException le) {
				Log.e(TAG, "getConnection", le);
				conn.close();
				throw le;
			}
		}

		return conn;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("LDAPServer(host=\"");
		buffer.append(host).append(":").append(port).append("\"");
		buffer.append(", bindDN=\"");
		if (bindDN != null) {
			buffer.append(bindDN);
		}
		buffer.append("\" - ");
		if (usesSSL()) {
			buffer.append("SSL");
		} else if (usesStartTLS()) {
			buffer.append("StartTLS");
		} else {
			buffer.append("No encryption");
		}
		buffer.append(")");
		return buffer.toString();
	}

	public boolean usesSSL() {
		return encryption == 1;
	}

	public boolean usesStartTLS() {
		return encryption == 2;
	}
}
