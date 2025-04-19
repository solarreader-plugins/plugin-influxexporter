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
package de.schnippsche.solarreader.test;

import de.schnippsche.solarreader.backend.connection.network.HttpConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import org.mockito.Mockito;

public class InfluxV2CorrectHttpConnection implements HttpConnection {

  @Override
  public void test(URL url, String validMediaType) {}

  @Override
  public HttpRequest buildGetRequest(URL url, Map<String, String> headers) {
    return null;
  }

  @Override
  public HttpRequest buildPostRequest(URL url, Map<String, String> headers, String body)
      throws URISyntaxException {
    HttpRequest.Builder requestBuilder =
        HttpRequest.newBuilder()
            .uri(url.toURI())
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
    if (headers != null) {
      headers.forEach(requestBuilder::header);
    }
    return requestBuilder.build();
  }

  @SuppressWarnings("unchecked")
  @Override
  public HttpResponse<String> get(URL url) {
    HttpResponse<String> httpResponseMock = Mockito.mock(HttpResponse.class);
    HttpHeaders headers = Mockito.mock(HttpHeaders.class);
    // Mock für HttpHeaders konfigurieren
    Mockito.when(headers.firstValue("X-Influxdb-Version")).thenReturn(Optional.of("v2.1"));
    Mockito.when(httpResponseMock.statusCode()).thenReturn(200); //
    Mockito.when(httpResponseMock.body()).thenReturn("Mocked Response Body");
    Mockito.when(httpResponseMock.headers()).thenReturn(headers);
    return httpResponseMock;
  }

  @Override
  public String getAsString(URL url) {
    return null;
  }

  @Override
  public HttpResponse<String> post(URL url, Map<String, String> formData, String body) {
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public HttpResponse<String> sendRequest(HttpRequest request) {
    HttpResponse<String> httpResponseMock = Mockito.mock(HttpResponse.class);
    HttpHeaders headers = Mockito.mock(HttpHeaders.class);
    // Mock für HttpHeaders konfigurieren
    Mockito.when(headers.firstValue("Content-Type")).thenReturn(Optional.of("application/json"));
    Mockito.when(httpResponseMock.body()).thenReturn("{ }");
    Mockito.when(httpResponseMock.statusCode()).thenReturn(200);
    return httpResponseMock;
  }
}
