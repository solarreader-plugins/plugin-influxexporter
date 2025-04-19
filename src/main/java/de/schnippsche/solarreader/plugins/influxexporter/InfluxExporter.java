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

import de.schnippsche.solarreader.backend.connection.general.ConnectionFactory;
import de.schnippsche.solarreader.backend.connection.network.HttpConnection;
import de.schnippsche.solarreader.backend.connection.network.HttpConnectionFactory;
import de.schnippsche.solarreader.backend.exporter.AbstractExporter;
import de.schnippsche.solarreader.backend.exporter.TransferData;
import de.schnippsche.solarreader.backend.protocol.KnownProtocol;
import de.schnippsche.solarreader.backend.provider.SupportedInterface;
import de.schnippsche.solarreader.backend.singleton.GlobalUsrStore;
import de.schnippsche.solarreader.backend.table.Table;
import de.schnippsche.solarreader.backend.table.TableCell;
import de.schnippsche.solarreader.backend.table.TableColumn;
import de.schnippsche.solarreader.backend.table.TableColumnType;
import de.schnippsche.solarreader.backend.table.TableRow;
import de.schnippsche.solarreader.backend.util.JsonTools;
import de.schnippsche.solarreader.backend.util.Setting;
import de.schnippsche.solarreader.frontend.ui.HtmlInputType;
import de.schnippsche.solarreader.frontend.ui.HtmlWidth;
import de.schnippsche.solarreader.frontend.ui.UIInputElementBuilder;
import de.schnippsche.solarreader.frontend.ui.UIList;
import de.schnippsche.solarreader.frontend.ui.UITextElementBuilder;
import de.schnippsche.solarreader.plugin.PluginMetadata;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import org.tinylog.Logger;

/**
 * InfluxExporter is responsible for exporting data to an InfluxDB instance.
 *
 * <p>This class provides methods to connect to an InfluxDB, configure the connection settings, and
 * export data from a source to the InfluxDB database. It extends the {@link AbstractExporter} class
 * and leverages its base functionality to implement the specifics of exporting data to InfluxDB.
 *
 * <p>The InfluxExporter includes support for various configurations, such as setting the host,
 * port, authentication details, database name, and SSL options for the InfluxDB connection.
 */
@PluginMetadata(
    name = "InfluxExporter",
    version = "1.0.1",
    author = "Stefan Töngi",
    url = "https://github.com/solarreader-plugins/plugin-InfluxExporter",
    svgImage = "influx.svg",
    supportedInterfaces = {SupportedInterface.NONE},
    usedProtocol = KnownProtocol.HTTP,
    supports = "Influx V1, V2")
public class InfluxExporter extends AbstractExporter {
  /** constant string for dbname */
  protected static final String DBNAME = "dbname";

  private static final String REQUIRED_ERROR = "influxexporter.required.error";
  private final ConnectionFactory<HttpConnection> connectionFactory;
  private final BlockingQueue<TransferData> queue;
  private InfluxData influxData;
  private HttpConnection httpConnection;
  private Thread consumerThread;
  private volatile boolean running = true;

  /** Constructs a new InfluxExporter with default configuration. */
  public InfluxExporter() {
    this(new HttpConnectionFactory());
  }

  /**
   * Constructs a new InfluxExporter with the specific connection factory.
   *
   * @param connectionFactory the factory for creating {@code HttpConnection} instances.
   */
  public InfluxExporter(ConnectionFactory<HttpConnection> connectionFactory) {
    super();
    this.connectionFactory = connectionFactory;
    this.queue = new LinkedBlockingQueue<>();
  }

  @Override
  public ResourceBundle getPluginResourceBundle() {
    return ResourceBundle.getBundle("influxexporter", locale);
  }

  /** Initializes the influx exporter by starting the consumer thread. */
  @Override
  public void initialize() {
    Logger.debug("initialize influx exporter");
    consumerThread = new Thread(this::processQueue);
    consumerThread.setName("InfluxExporterThread");
    consumerThread.start();
  }

  @Override
  public void shutdown() {
    running = false;
    consumerThread.interrupt();
  }

  /** Shuts down the influx exporter by stopping the consumer thread. */
  @Override
  public void addExport(TransferData transferData) {
    if (transferData.getTables().isEmpty()) {
      Logger.debug("no exporting tables, skip export");
      return;
    }
    Logger.debug("add export to '{}'", exporterData.getName());
    exporterData.setLastCall(transferData.getTimestamp());
    queue.add(transferData);
  }

  /**
   * Tests the connection to the exporter with the provided configuration.
   *
   * @param testSetting the configuration setting.
   * @return an empty string if the connection test is successful.
   * @throws IOException if the directory does not exist, is not a directory, or is not writable.
   */
  @Override
  public String testExporterConnection(Setting testSetting) throws IOException {
    InfluxData testInfluxData = new InfluxData(testSetting);
    try {
      String version = getInfluxVersion(testInfluxData);
      testInfluxData.setVersion(version);
      HttpConnection testConnection = connectionFactory.createConnection(testSetting);
      final HttpRequest testRequest = buildRequest(testConnection, testInfluxData, "");
      HttpResponse<String> response = testConnection.sendRequest(testRequest);
      if (response.statusCode() >= 200 && response.statusCode() <= 300) {
        String message = resourceBundle.getString("influxexporter.connection.successful");
        return MessageFormat.format(message, version);
      }
      Logger.error(response.statusCode());
      String ct = response.headers().firstValue(HttpConnection.CONTENT_TYPE).orElse("");
      if (ct.contains(HttpConnection.CONTENT_TYPE_JSON)) {
        Map<String, Object> jsonError = new JsonTools().getSimpleMapFromJsonString(response.body());
        String error =
            Objects.toString(
                jsonError.getOrDefault(
                    "error",
                    jsonError.getOrDefault(
                        "message", "unknown json error with " + response.statusCode())));
        throw new IOException(error);
      }
      String error = "" + response.statusCode();
      throw new IOException(error);
    } catch (MalformedURLException | URISyntaxException e) {
      throw new IOException("malformed url");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("connection timeout");
    }
  }

  /**
   * Retrieves the exporter dialog for the current locale.
   *
   * @return an optional UIList containing the dialog elements.
   */
  @Override
  public Optional<UIList> getExporterDialog() {
    UIList uiList = new UIList();
    uiList.addElement(
        new UITextElementBuilder()
            .withLabel(resourceBundle.getString("influxexporter.title"))
            .build());
    uiList.addElement(
        new UIInputElementBuilder()
            .withName(Setting.PROVIDER_HOST)
            .withType(HtmlInputType.TEXT)
            .withColumnWidth(HtmlWidth.HALF)
            .withRequired(true)
            .withTooltip(resourceBundle.getString("influxexporter.host.tooltip"))
            .withLabel(resourceBundle.getString("influxexporter.host.text"))
            .withPlaceholder(resourceBundle.getString("influxexporter.host.text"))
            .withInvalidFeedback(resourceBundle.getString(REQUIRED_ERROR))
            .build());
    uiList.addElement(
        new UIInputElementBuilder()
            .withName(Setting.PROVIDER_PORT)
            .withType(HtmlInputType.NUMBER)
            .withColumnWidth(HtmlWidth.HALF)
            .withRequired(true)
            .withTooltip(resourceBundle.getString("influxexporter.port.tooltip"))
            .withLabel(resourceBundle.getString("influxexporter.port.text"))
            .withPlaceholder(resourceBundle.getString("influxexporter.port.text"))
            .withInvalidFeedback(resourceBundle.getString(REQUIRED_ERROR))
            .build());
    uiList.addElement(
        new UIInputElementBuilder()
            .withName(Setting.OPTIONAL_USER)
            .withType(HtmlInputType.TEXT)
            .withColumnWidth(HtmlWidth.HALF)
            .withRequired(false)
            .withTooltip(resourceBundle.getString("influxexporter.user.tooltip"))
            .withLabel(resourceBundle.getString("influxexporter.user.text"))
            .withPlaceholder(resourceBundle.getString("influxexporter.user.text"))
            .build());
    uiList.addElement(
        new UIInputElementBuilder()
            .withName(Setting.OPTIONAL_PASSWORD)
            .withType(HtmlInputType.TEXT)
            .withColumnWidth(HtmlWidth.HALF)
            .withRequired(false)
            .withTooltip(resourceBundle.getString("influxexporter.password.tooltip"))
            .withLabel(resourceBundle.getString("influxexporter.password.text"))
            .withPlaceholder(resourceBundle.getString("influxexporter.password.text"))
            .build());
    uiList.addElement(
        new UIInputElementBuilder()
            .withName(DBNAME)
            .withType(HtmlInputType.TEXT)
            .withColumnWidth(HtmlWidth.HALF)
            .withRequired(true)
            .withTooltip(resourceBundle.getString("influxexporter.name.tooltip"))
            .withLabel(resourceBundle.getString("influxexporter.name.text"))
            .withPlaceholder(resourceBundle.getString("influxexporter.name.text"))
            .withInvalidFeedback(resourceBundle.getString(REQUIRED_ERROR))
            .build());
    uiList.addElement(
        new UIInputElementBuilder()
            .withName(Setting.USE_SSL)
            .withType(HtmlInputType.CHECKBOX)
            .withColumnWidth(HtmlWidth.HALF)
            .withTooltip(resourceBundle.getString("influxexporter.use.ssl.tooltip"))
            .withLabel(resourceBundle.getString("influxexporter.use.ssl.label"))
            .withPlaceholder(resourceBundle.getString("influxexporter.use.ssl.text"))
            .build());
    return Optional.of(uiList);
  }

  /**
   * Retrieves the default exporter configuration.
   *
   * @return a map containing the default configuration parameters.
   */
  @Override
  public Setting getDefaultExporterSetting() {
    return new InfluxData().getSetting();
  }

  /** Processes the export queue by taking each entry and exporting it. */
  private void processQueue() {
    while (running) {
      try {
        TransferData transferData = queue.take();
        doStandardExport(transferData);
      } catch (InterruptedException e) {
        if (!running) {
          break; // Exit loop if not running
        }
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Retrieves the InfluxDB version from the specified InfluxData configuration.
   *
   * @param influxData the configuration data for connecting to InfluxDB
   * @return the version of the InfluxDB server
   * @throws IOException if an I/O error occurs while retrieving the version
   * @throws InterruptedException if the operation is interrupted
   */
  private String getInfluxVersion(InfluxData influxData) throws IOException, InterruptedException {
    try {
      URL connectionUrl = influxData.getConnectionUrl();
      Logger.debug("test url = {}", connectionUrl);
      Setting testSetting = influxData.getSetting();
      HttpConnection testConnection = connectionFactory.createConnection(testSetting);
      HttpResponse<String> response = testConnection.get(connectionUrl);
      String influxVersion = response.headers().firstValue("X-Influxdb-Version").orElse("unknown");
      Logger.debug("influx Version={}", influxVersion);
      return influxVersion;
    } catch (MalformedURLException e) {
      throw new IOException("URL is wrong");
    }
  }

  /**
   * Creates the request string in the InfluxDB line protocol format from the specified table.
   *
   * @param table the table containing the data
   * @param timestampColumn the column containing the timestamp, if any
   * @param currentTimestampSeconds the current timestamp in seconds
   * @param builder the StringBuilder to which the request string is appended
   */
  private void createRequestString(
      Table table,
      TableColumn timestampColumn,
      long currentTimestampSeconds,
      StringBuilder builder) {
    for (TableRow tableRow : table.getRows()) {
      List<String> colData =
          table.getColumnsWithoutTimestamp().stream()
              .map(column -> getCalculatedString(table, column, tableRow))
              .filter(Objects::nonNull)
              .collect(Collectors.toList());
      if (!colData.isEmpty()) {
        Optional<TableCell> optionalTableCell = table.getTableCell(timestampColumn, tableRow);
        long timestamp =
            optionalTableCell
                .map(TableCell::getCalculatedAsTimestampSeconds)
                .orElse(currentTimestampSeconds);
        builder
            .append(table.getTableName())
            .append(" ")
            .append(String.join(",", colData))
            .append(" ")
            .append(timestamp)
            .append("\n");
      }
    }
  }

  /**
   * Calculates the string representation of the given table column and table row.
   *
   * <p>This method retrieves the calculated string value from the table row for the specified table
   * column. If the column type is STRING, it formats the result as "columnName=\"value\"". For
   * other column types, it formats the result as "columnName=value".
   *
   * @param tableColumn the table column to be processed
   * @param tableRow the table row containing the data
   * @return the calculated string representation of the column data, or null if the calculated
   *     string is null
   */
  private String getCalculatedString(Table table, TableColumn tableColumn, TableRow tableRow) {
    Optional<TableCell> optionalTableCell = table.getTableCell(tableColumn, tableRow);
    if (optionalTableCell.isEmpty()) {
      return null;
    }

    String calculatedString = optionalTableCell.get().getCalculatedAsString();
    if (calculatedString == null) {
      return null;
    }
    if (TableColumnType.STRING == tableColumn.getColumnType()) {
      return tableColumn.getColumnName() + "=\"" + calculatedString + "\"";
    } else if (calculatedString.trim().isEmpty()) {
      return null;
    }

    return tableColumn.getColumnName() + "=" + calculatedString;
  }

  /**
   * Sends the specified data to InfluxDB using the given InfluxData configuration.
   *
   * <p>This method checks the version of the InfluxDB server and constructs the appropriate
   * request. If an error occurs during the process, it logs the error and throws an IOException.
   *
   * @param influxData the configuration data for connecting to InfluxDB
   * @param data the data to be sent to InfluxDB
   * @throws IOException if an I/O error occurs during the request
   */
  private void sendRequest(HttpConnection tempHttpConnection, InfluxData influxData, String data)
      throws IOException {
    if (influxData.getVersion() == null) {
      try {
        influxData.setVersion(getInfluxVersion(influxData));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        Logger.error("send request interrupted:{}", e.getMessage());
      } catch (IOException e) {
        Logger.error("database error:{}", e.getMessage());
      }
    }
    Logger.debug("build data {}", data.replace("\n", ""));
    try {
      HttpRequest httpRequest = buildRequest(tempHttpConnection, influxData, data);
      HttpResponse<String> response = tempHttpConnection.sendRequest(httpRequest);
      if (response.statusCode() >= 300) {
        Logger.error("Influx returns error code {}, data={}", response.statusCode(), data);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("interrupted");
    } catch (URISyntaxException e) {
      throw new IOException("invalid url");
    }
  }

  /**
   * Builds an HTTP request for sending data to InfluxDB based on the specified InfluxData
   * configuration.
   *
   * <p>This method constructs the appropriate URL and authorization headers based on the version of
   * InfluxDB. If the version is unsupported or unknown, it logs an error and returns null.
   *
   * @param tempHttpConnection the HttpConnection for connecting
   * @param influxData the configuration data for connecting to InfluxDB
   * @param data the data to be sent to InfluxDB
   * @return the constructed HttpRequest object
   * @throws URISyntaxException if the constructed URL is invalid
   */
  private HttpRequest buildRequest(
      HttpConnection tempHttpConnection, InfluxData influxData, String data)
      throws URISyntaxException {
    try {
      URL url;
      Map<String, String> headers;
      InfluxVersion influxVersion;
      if (influxData.getMajorVersion() == 1) {
        influxVersion = new InfluxVersionV1();
      } else if (influxData.getMajorVersion() == 2) {
        influxVersion = new InfluxVersionV2();
      } else {
        Logger.error("unsupported or unknown influx DB version '{}'", influxData.getVersion());
        return null;
      }
      url = influxVersion.buildInfluxUrl(influxData);
      Logger.debug("url:{}, data:{}", url, data);
      headers = influxVersion.getAuthorization(influxData);
      return tempHttpConnection.buildPostRequest(url, headers, data);
    } catch (MalformedURLException e) {
      Logger.error("malformed url exception", e.getMessage());
    }
    return null;
  }

  /**
   * Exports the data from the specified table to the InfluxDB.
   *
   * <p>This method converts the table data into the InfluxDB line protocol format and sends the
   * data to InfluxDB. If the table contains a timestamp column, it uses that; otherwise, it uses
   * the current timestamp. If the table is empty, the export is skipped.
   *
   * @param table the table containing the data to be exported
   * @param zonedDateTime the timestamp to be used for the data points
   * @throws IOException if an I/O error occurs during the export
   */
  @Override
  protected void exportTable(Table table, ZonedDateTime zonedDateTime) throws IOException {
    long currentTimestampSeconds = GlobalUsrStore.getInstance().getUtcTimestamp(zonedDateTime);
    long startTime = System.currentTimeMillis();
    StringBuilder builder = new StringBuilder();
    // last column is timestamp if exists or new column
    TableColumn timestampColumn = table.getTimestampColumn();
    createRequestString(table, timestampColumn, currentTimestampSeconds, builder);
    if (builder.length() > 0) {
      sendRequest(httpConnection, influxData, builder.toString());
    } else {
      Logger.warn("empty table(s), skip export");
    }
    Logger.debug(
        "export table '{}' to '{}' finished in {} ms",
        table.getTableName(),
        exporterData.getName(),
        (System.currentTimeMillis() - startTime));
  }

  /** Updates the configuration of the exporter based on the exporter data. */
  protected void updateConfiguration() {
    this.influxData = new InfluxData(exporterData.getSetting());
    httpConnection = connectionFactory.createConnection(exporterData.getSetting());
  }
}
