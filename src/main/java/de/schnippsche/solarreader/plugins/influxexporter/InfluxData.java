/*
 * Copyright (c) 2024-2025 Stefan Toengi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.schnippsche.solarreader.plugins.influxexporter;

import de.schnippsche.solarreader.backend.util.Setting;
import de.schnippsche.solarreader.backend.util.StringConverter;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Represents the configuration details for connecting to an InfluxDB instance.
 *
 * <p>This class encapsulates the information required to connect to an InfluxDB provider, including
 * connection details like the host, port, authentication credentials, database name, SSL settings,
 * and read timeout. The class provides methods to retrieve and modify these settings, as well as
 * construct a connection URL.
 */
public class InfluxData {
  private final String host;
  private final int port;
  private final String user;
  private final String password;
  private final String dbName;
  private final boolean ssl;
  private final int readTimeout;
  private String version;

  /**
   * Default constructor that initializes the InfluxData with default values.
   *
   * <ul>
   *   <li>host: "localhost"
   *   <li>port: 8086
   *   <li>readTimeout: 5000 milliseconds
   *   <li>user: null
   *   <li>password: null
   *   <li>dbName: "solarreader"
   *   <li>ssl: false
   *   <li>version: null
   * </ul>
   */
  public InfluxData() {
    host = "localhost";
    port = 8086;
    readTimeout = 5000;
    user = null;
    password = null;
    dbName = "solarreader";
    ssl = false;
    version = null;
  }

  /**
   * Constructs an InfluxData object using the provided Setting object. This constructor populates
   * the InfluxData fields from the provided configuration settings.
   *
   * @param setting the Setting object containing the configuration values to initialize the
   *     InfluxData.
   */
  public InfluxData(Setting setting) {
    host = setting.getProviderHost();
    port = setting.getProviderPort();
    readTimeout = setting.getReadTimeoutMilliseconds();
    user = setting.getOptionalUser();
    password = setting.getOptionalPassword();
    dbName = setting.getConfigurationValueAsString(InfluxExporter.DBNAME, "solarreader");
    ssl = setting.isSsl();
    version = null;
  }

  /**
   * Returns the current configuration as a Setting object. The returned Setting object contains all
   * the configuration values for the InfluxData instance.
   *
   * @return a Setting object with the current configuration of the InfluxData instance.
   */
  public Setting getSetting() {
    Setting setting = new Setting();
    setting.setProviderHost(host);
    setting.setProviderPort(port);
    setting.setOptionalUser(user);
    setting.setOptionalPassword(password);
    setting.setReadTimeoutMilliseconds(readTimeout);
    setting.setConfigurationValue(InfluxExporter.DBNAME, dbName);
    setting.setSsl(ssl);
    return setting;
  }

  /**
   * Gets the host of the InfluxDB provider.
   *
   * @return the host address of the InfluxDB provider.
   */
  public String getHost() {
    return host;
  }

  /**
   * Gets the port of the InfluxDB provider.
   *
   * @return the port number of the InfluxDB provider.
   */
  public int getPort() {
    return port;
  }

  /**
   * Gets the user name for authentication with the InfluxDB provider.
   *
   * @return the user name for authentication, or null if no user is provided.
   */
  public String getUser() {
    return user;
  }

  /**
   * Gets the password for authentication with the InfluxDB provider.
   *
   * @return the password for authentication, or null if no password is provided.
   */
  public String getPassword() {
    return password;
  }

  /**
   * Gets the name of the InfluxDB database to connect to.
   *
   * @return the name of the InfluxDB database.
   */
  public String getDbName() {
    return dbName;
  }

  /**
   * Checks whether SSL is enabled for the connection.
   *
   * @return {@code true} if SSL is enabled; {@code false} otherwise.
   */
  public boolean isSsl() {
    return ssl;
  }

  /**
   * Constructs and returns the URL for connecting to the InfluxDB instance. The URL is built based
   * on the host, port, and SSL settings.
   *
   * <p>If SSL is enabled, the URL will use "https"; otherwise, it uses "http".
   *
   * @return the connection URL for the InfluxDB provider.
   * @throws MalformedURLException if the URL cannot be constructed due to invalid format.
   */
  public URL getConnectionUrl() throws MalformedURLException {
    return new URL(String.format("http%s://%s:%s/", (ssl) ? "s" : "", host, port));
  }

  /**
   * Gets the version of the InfluxDB instance.
   *
   * @return the version string of the InfluxDB, or null if not set.
   */
  public String getVersion() {
    return version;
  }

  /**
   * Sets the version of the InfluxDB instance.
   *
   * @param version the version string of the InfluxDB to be set.
   */
  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * Returns the major version number from the InfluxDB version string.
   *
   * <p>The version string is assumed to follow the pattern "X.Y.Z", where "X" is the major version.
   * If the version string is invalid or contains non-numeric characters, it will return 1.
   *
   * @return the major version number of the InfluxDB instance.
   */
  public int getMajorVersion() {
    String versionString = version.replaceAll("[^0-9.]", "") + ".";
    String[] parts = versionString.split("\\.");
    return new StringConverter(parts[0]).toInt(1);
  }
}
