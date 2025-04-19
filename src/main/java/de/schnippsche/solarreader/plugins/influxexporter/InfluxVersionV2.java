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
import java.util.Collections;
import java.util.Map;

/**
 * Implementation of the InfluxVersion interface for InfluxDB version 2.x.
 *
 * <p>This class provides methods specific to handling and interacting with InfluxDB version 2.x.
 */
public class InfluxVersionV2 implements InfluxVersion {
  /**
   * Builds the URL for writing data to InfluxDB v2.x based on the provided InfluxData.
   *
   * @param influxData the configuration data for connecting to InfluxDB
   * @return the URL for writing data to InfluxDB v2.x
   * @throws MalformedURLException if the constructed URL is malformed
   */
  @Override
  public URL buildInfluxUrl(InfluxData influxData) throws MalformedURLException {
    return new URL(
        String.format(
            "http%s://%s:%s/api/v2/write?bucket=%s&precision=s&org=%s",
            (influxData.isSsl()) ? "s" : "",
            influxData.getHost(),
            influxData.getPort(),
            influxData.getDbName(),
            influxData.getUser()));
  }

  /**
   * Generates the authorization headers for InfluxDB v2.x based on the provided InfluxData.
   *
   * @param influxData the configuration data containing the user credentials
   * @return a map containing the authorization headers
   */
  @Override
  public Map<String, String> getAuthorization(InfluxData influxData) {
    if (influxData.getUser() != null && influxData.getPassword() != null) {
      return Map.of("Authorization", "Token " + influxData.getPassword());
    }
    return Collections.emptyMap();
  }
}
