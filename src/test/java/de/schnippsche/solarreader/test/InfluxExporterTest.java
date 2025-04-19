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

import de.schnippsche.solarreader.backend.connection.general.ConnectionFactory;
import de.schnippsche.solarreader.backend.connection.network.HttpConnection;
import de.schnippsche.solarreader.database.ExporterData;
import de.schnippsche.solarreader.plugins.influxexporter.InfluxExporter;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class InfluxExporterTest {
  @Test
  void testInfluxExport() throws IOException {
    GeneralTestHelper generalTestHelper = new GeneralTestHelper();
    ExporterData exporterData = new ExporterData();
    exporterData.setName("InfluxTest");
    // test correct v1 influx db
    ConnectionFactory<HttpConnection> testFactoryV1Correct =
        knownConfiguration -> new InfluxV1CorrectHttpConnection();
    InfluxExporter exporterV1Correct = new InfluxExporter(testFactoryV1Correct);
    exporterData.setSetting(exporterV1Correct.getDefaultExporterSetting());
    exporterV1Correct.setExporterData(exporterData);
    generalTestHelper.testExporterInterface(exporterV1Correct);
    // test wrong v1 influx db
    ConnectionFactory<HttpConnection> testFactoryV1Wrong =
        knownConfiguration -> new InfluxV1WrongHttpConnection();
    InfluxExporter exporterV1Wrong = new InfluxExporter(testFactoryV1Wrong);
    exporterData.setSetting(exporterV1Wrong.getDefaultExporterSetting());
    exporterV1Wrong.setExporterData(exporterData);
    // test correct v2 influx db
    ConnectionFactory<HttpConnection> testFactoryV2Correct =
        knownConfiguration -> new InfluxV2CorrectHttpConnection();
    InfluxExporter exporterV2Correct = new InfluxExporter(testFactoryV2Correct);
    exporterData.setSetting(exporterV2Correct.getDefaultExporterSetting());
    exporterV2Correct.setExporterData(exporterData);
    generalTestHelper.testExporterInterface(exporterV2Correct);

    try {
      generalTestHelper.testExporterInterface(exporterV1Wrong);
      throw new RuntimeException("error handling not correct ");
    } catch (IOException e) {
      assert ("database not found: test".equals(e.getMessage()));
    }
  }
}
