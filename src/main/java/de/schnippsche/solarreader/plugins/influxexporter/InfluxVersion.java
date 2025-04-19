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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * Represents an interface for managing version-specific interactions with an InfluxDB instance.
 *
 * <p>This interface defines methods for building the InfluxDB connection URL based on
 * version-specific parameters and retrieving authorization details for connecting to the InfluxDB
 * instance. Implementations of this interface will provide version-specific logic for interacting
 * with InfluxDB based on the version of the InfluxDB instance in use.
 */
public interface InfluxVersion {

  /**
   * Builds the connection URL for InfluxDB based on the provided configuration settings. This
   * method constructs a URL to connect to the InfluxDB instance. The URL format may vary depending
   * on the version of InfluxDB, and the implementation will account for version-specific
   * differences.
   *
   * @param influxData the configuration settings for the InfluxDB connection, including host, port,
   *     SSL, etc.
   * @return the connection URL for the InfluxDB instance
   * @throws MalformedURLException if the URL format is incorrect or cannot be constructed
   */
  URL buildInfluxUrl(InfluxData influxData) throws MalformedURLException;

  /**
   * Retrieves the authorization details for connecting to the InfluxDB instance. This method
   * returns a map of authorization headers or parameters needed to authenticate with the InfluxDB
   * instance. The authorization process may differ depending on the InfluxDB version.
   *
   * @param influxData the configuration settings, including user credentials, for InfluxDB
   *     connection
   * @return a map containing authorization headers or parameters for the InfluxDB connection
   */
  Map<String, String> getAuthorization(InfluxData influxData);
}
